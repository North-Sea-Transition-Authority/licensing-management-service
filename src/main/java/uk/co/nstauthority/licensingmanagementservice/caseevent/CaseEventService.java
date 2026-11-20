package uk.co.nstauthority.licensingmanagementservice.caseevent;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.audit.AuditRevisionUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseAllocationPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseEventPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseNotePayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateRequestPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateSubmittedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspCorrectedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.Application;
import uk.co.nstauthority.licensingmanagementservice.summary.UploadedFileView;

@Service
public class CaseEventService {

  static final String CASE_EVENT_USER_PURPOSE = "Fetch user names for application case events";
  static final String SYSTEM_USER_DISPLAY_NAME = "System";
  static final String ALLOCATED_TO_KEY = "Allocated to";
  static final String UPDATE_REASON_KEY = "Update reason";
  static final String UPDATE_DEADLINE_KEY = "Update deadline";
  static final String VERSION_KEY = "Version";
  static final String DSP_KEY = "DSP";
  static final String REPLACED_DSP_KEY = "Replaced DSP";
  static final String CORRECTED_DSP_KEY = "Corrected DSP";

  private final CaseEventRepository caseEventRepository;
  private final UserDetailService userDetailService;
  private final EnergyPortalUserService energyPortalUserService;
  private final FileService fileService;
  private final Clock clock;

  public CaseEventService(
      CaseEventRepository caseEventRepository,
      UserDetailService userDetailService,
      EnergyPortalUserService energyPortalUserService,
      FileService fileService,
      Clock clock
  ) {
    this.caseEventRepository = caseEventRepository;
    this.userDetailService = userDetailService;
    this.energyPortalUserService = energyPortalUserService;
    this.fileService = fileService;
    this.clock = clock;
  }

  @Transactional
  public CaseEvent recordCaseEvent(
      CaseEventType eventType,
      Application application,
      @Nullable CaseEventPayload payload
  ) {
    var caseEvent = new CaseEvent(
        application.getApplicationType(),
        application.getId(),
        eventType,
        clock.instant(),
        getCurrentUser(),
        payload
    );
    return caseEventRepository.save(caseEvent);
  }

  public List<CaseEventView> getCaseEventViews(Application application) {
    return getCaseEventViews(application, fileId -> {
      throw new IllegalStateException("No download route for case event file " + fileId);
    });
  }

  public List<CaseEventView> getCaseEventViews(
      Application application,
      Function<UUID, String> downloadUrlByFileId
  ) {
    var caseEvents = findCaseEvents(application);

    if (caseEvents.isEmpty()) {
      return List.of();
    }

    // One call to the Energy Portal for every user, rather than one per event.
    var usersByWuaId = energyPortalUserService.getEnergyPortalUserMap(
        getUserWuaIds(caseEvents),
        CASE_EVENT_USER_PURPOSE
    );

    var filesById = getFilesById(caseEvents);

    return caseEvents.stream()
        .map(caseEvent -> createViewFrom(caseEvent, usersByWuaId, filesById, downloadUrlByFileId))
        .toList();
  }

  public boolean isFileOnApplicationEvent(UUID fileId, Application application) {
    return findCaseEvents(application).stream()
        .anyMatch(caseEvent -> getFileIdsByLabel(caseEvent.getPayload()).values().stream()
            .anyMatch(fileIds -> fileIds.contains(fileId)));
  }

  private List<CaseEvent> findCaseEvents(Application application) {
    return caseEventRepository.findByApplicationTypeAndApplicationIdOrderByEventInstantDesc(
        application.getApplicationType(),
        application.getId()
    );
  }

  private Map<UUID, UploadedFile> getFilesById(List<CaseEvent> caseEvents) {
    var fileIds = caseEvents.stream()
        .flatMap(caseEvent -> getFileIdsByLabel(caseEvent.getPayload()).values().stream())
        .flatMap(List::stream)
        .distinct()
        .toList();

    if (fileIds.isEmpty()) {
      return Map.of();
    }

    return fileService.findAll(fileIds).stream()
        .collect(Collectors.toMap(UploadedFile::getId, Function.identity()));
  }

  private static Map<String, List<UUID>> getFileIdsByLabel(CaseEventPayload payload) {
    return switch (payload) {
      case DspUploadedPayload(var fileIds) -> Map.of(DSP_KEY, fileIds);
      case DspCorrectedPayload(var replacedFileIds, var correctedFileIds) -> {
        var fileIdsByLabel = new LinkedHashMap<String, List<UUID>>();
        fileIdsByLabel.put(REPLACED_DSP_KEY, replacedFileIds);
        fileIdsByLabel.put(CORRECTED_DSP_KEY, correctedFileIds);
        yield fileIdsByLabel;
      }
      case CaseNotePayload ignored -> Map.of();
      case CaseAllocationPayload ignored -> Map.of();
      case CaseUpdateRequestPayload ignored -> Map.of();
      case CaseUpdateSubmittedPayload ignored -> Map.of();
      case null -> Map.of();
    };
  }

  private List<WebUserAccountId> getUserWuaIds(List<CaseEvent> caseEvents) {
    return caseEvents.stream()
        .flatMap(caseEvent -> getWuaIdsOnEvent(caseEvent).stream())
        .filter(Objects::nonNull)
        .distinct()
        .map(WebUserAccountId::from)
        .toList();
  }

  private static List<Long> getWuaIdsOnEvent(CaseEvent caseEvent) {
    var wuaIds = new ArrayList<Long>();
    wuaIds.add(getDisplayUserWuaId(caseEvent));
    if (caseEvent.getPayload() instanceof CaseAllocationPayload(Long allocatedToWuaId)) {
      wuaIds.add(allocatedToWuaId);
    }
    return wuaIds;
  }

  private CaseEventUser getCurrentUser() {
    var loggedInUser = userDetailService.findUserDetail();
    if (loggedInUser.isPresent()) {
      return CaseEventUser.from(loggedInUser.get());
    }

    var fallbackUser = AuditRevisionUtil.getFallbackAuditUser();
    if (fallbackUser != null) {
      return CaseEventUser.from(fallbackUser);
    }

    return CaseEventUser.system();
  }

  private CaseEventView createViewFrom(
      CaseEvent caseEvent,
      Map<WebUserAccountId, EnergyPortalUserJson> usersByWuaId,
      Map<UUID, UploadedFile> filesById,
      Function<UUID, String> downloadUrlByFileId
  ) {
    return new CaseEventView(
        caseEvent.getEventType(),
        getDetails(caseEvent.getPayload(), usersByWuaId),
        getFiles(caseEvent.getPayload(), filesById, downloadUrlByFileId),
        getEventBy(caseEvent, usersByWuaId),
        caseEvent.getEventInstant()
    );
  }

  private static String getEventBy(CaseEvent caseEvent, Map<WebUserAccountId, EnergyPortalUserJson> usersByWuaId) {
    if (caseEvent.isSystem()) {
      return SYSTEM_USER_DISPLAY_NAME;
    }

    return getUserDisplayText(getDisplayUserWuaId(caseEvent), usersByWuaId);
  }

  private static Map<String, String> getDetails(
      CaseEventPayload payload,
      Map<WebUserAccountId, EnergyPortalUserJson> usersByWuaId
  ) {
    var details = new LinkedHashMap<String, String>();
    if (payload instanceof CaseAllocationPayload(Long allocatedToWuaId)) {
      details.put(ALLOCATED_TO_KEY, getUserDisplayText(allocatedToWuaId, usersByWuaId));
    }
    if (payload instanceof CaseUpdateRequestPayload(var updateDeadlineDate, var updateReason)) {
      details.put(UPDATE_DEADLINE_KEY, DateFormatUtil.convertToDisplayText(updateDeadlineDate));
      details.put(UPDATE_REASON_KEY, updateReason);
    }
    if (payload instanceof CaseUpdateSubmittedPayload(var versionNumber)) {
      details.put(VERSION_KEY, String.valueOf(versionNumber));
    }
    return details;
  }

  private static Map<String, List<UploadedFileView>> getFiles(
      CaseEventPayload payload,
      Map<UUID, UploadedFile> filesById,
      Function<UUID, String> downloadUrlByFileId
  ) {
    var files = new LinkedHashMap<String, List<UploadedFileView>>();
    getFileIdsByLabel(payload).forEach((label, fileIds) -> {
      var fileViews = fileIds.stream()
          .map(filesById::get)
          .filter(Objects::nonNull)
          .map(file -> UploadedFileView.from(file, downloadUrlByFileId.apply(file.getId())))
          .toList();
      if (!fileViews.isEmpty()) {
        files.put(label, fileViews);
      }
    });
    return files;
  }

  private static String getUserDisplayText(Long wuaId, Map<WebUserAccountId, EnergyPortalUserJson> usersByWuaId) {
    var user = usersByWuaId.get(WebUserAccountId.from(wuaId));
    if (user == null) {
      return null;
    }

    return "%s (%s)".formatted(user.displayName(), user.emailAddress());
  }

  private static Long getDisplayUserWuaId(CaseEvent caseEvent) {
    return caseEvent.getProxyWuaId() != null
        ? caseEvent.getProxyWuaId()
        : caseEvent.getUserWuaId();
  }
}
