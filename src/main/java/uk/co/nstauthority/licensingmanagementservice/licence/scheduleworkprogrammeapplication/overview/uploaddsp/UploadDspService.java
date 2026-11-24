package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspAddendumUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspCorrectedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp.CurrentDspView.DspAddendumView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp.CurrentDspView.DspFileView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class UploadDspService {

  static final String UPLOADER_USER_PURPOSE = "Fetch the name of the user who uploaded the DSP";

  private final ScheduleWorkProgrammeApplicationDetailRepository detailRepository;
  private final ApplicationFileService applicationFileService;
  private final ApplicationLetterService applicationLetterService;
  private final CaseEventService caseEventService;
  private final EnergyPortalUserService energyPortalUserService;

  public UploadDspService(
      ScheduleWorkProgrammeApplicationDetailRepository detailRepository,
      ApplicationFileService applicationFileService,
      ApplicationLetterService applicationLetterService,
      CaseEventService caseEventService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.detailRepository = detailRepository;
    this.applicationFileService = applicationFileService;
    this.applicationLetterService = applicationLetterService;
    this.caseEventService = caseEventService;
    this.energyPortalUserService = energyPortalUserService;
  }

  public UploadDspForm getFormForApplication(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new UploadDspForm();

    if (applicationDetail.getDecisionDate() != null) {
      form.getDecisionDate().setDate(applicationDetail.getDecisionDate());
    }

    var uploadedFiles = applicationFileService.getUploadedFiles(
        UploadDspFileUsage.fromApplication(applicationDetail))
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();
    form.setFinalDecisionSupportPapers(uploadedFiles);

    return form;
  }

  @Transactional
  public void uploadDsp(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      UploadDspForm form) {
    applicationLetterService.createDocumentInstance(applicationDetail.getScheduleWorkProgrammeApplication());
    form.getDecisionDate().getAsLocalDate().ifPresent(applicationDetail::setDecisionDate);
    applicationDetail.setStatus(ApplicationStatus.DSP_UPLOADED);
    detailRepository.save(applicationDetail);

    applicationFileService.saveDocuments(
        UploadDspFileUsage.fromApplication(applicationDetail),
        form.getFinalDecisionSupportPapers()
    );

    caseEventService.recordCaseEvent(
        CaseEventType.DSP_UPLOADED,
        applicationDetail.getScheduleWorkProgrammeApplication(),
        new DspUploadedPayload(getFileIds(form.getFinalDecisionSupportPapers()))
    );
  }

  public CurrentDspView getCurrentDsp(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var files = applicationFileService.getUploadedFiles(UploadDspFileUsage.fromApplication(applicationDetail));

    var fileViews = files.stream()
        .map(file -> new DspFileView(
            file.getName(),
            ReverseRouter.route(on(UploadDspController.class)
                .downloadFile(file.getId(), applicationDetail.getId(), null, null)),
            file.getDescription()
        ))
        .toList();

    var addendumFiles = applicationFileService.getUploadedFiles(UploadDspFileUsage.addendumFromApplication(applicationDetail))
        .stream()
        .sorted(Comparator.comparing(UploadedFile::getUploadedAt))
        .toList();
    var latestDspFile = files.stream().max(Comparator.comparing(UploadedFile::getUploadedAt));

    var uploaders = getUploaders(Stream.concat(latestDspFile.stream(), addendumFiles.stream()).toList());

    var addendumViews = addendumFiles.stream()
        .map(file -> new DspAddendumView(
            file.getName(),
            ReverseRouter.route(on(UploadDspController.class)
                .downloadAddendum(file.getId(), applicationDetail.getId(), null, null)),
            file.getDescription(),
            getUploaderName(file, uploaders),
            DateFormatUtil.convertToDisplayTextWithTime(file.getUploadedAt())
        ))
        .toList();

    var uploadedBy = latestDspFile
        .map(file -> "%s, %s".formatted(
            getUploaderName(file, uploaders),
            DateFormatUtil.convertToDisplayTextWithTime(file.getUploadedAt())))
        .orElse(null);

    return new CurrentDspView(
        DateFormatUtil.convertToDisplayText(applicationDetail.getDecisionDate()),
        fileViews,
        addendumViews,
        uploadedBy
    );
  }

  @Transactional
  public void correctDsp(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      UploadDspForm form) {
    var fileUsage = UploadDspFileUsage.fromApplication(applicationDetail);
    var replacedFileIds = applicationFileService
        .moveFiles(fileUsage, UploadDspFileUsage.supersededFromApplication(applicationDetail))
        .stream()
        .map(UploadedFile::getId)
        .toList();
    applicationFileService.saveDocuments(fileUsage, form.getFinalDecisionSupportPapers());

    caseEventService.recordCaseEvent(
        CaseEventType.DSP_CORRECTED,
        applicationDetail.getScheduleWorkProgrammeApplication(),
        new DspCorrectedPayload(replacedFileIds, getFileIds(form.getFinalDecisionSupportPapers()))
    );
  }

  @Transactional
  public void addAddendum(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      UploadDspForm form) {
    applicationFileService.saveDocuments(
        UploadDspFileUsage.addendumFromApplication(applicationDetail),
        form.getDspAddendums()
    );

    caseEventService.recordCaseEvent(
        CaseEventType.DSP_ADDENDUM_UPLOADED,
        applicationDetail.getScheduleWorkProgrammeApplication(),
        new DspAddendumUploadedPayload(getFileIds(form.getDspAddendums()))
    );
  }

  private static List<UUID> getFileIds(List<UploadedFileForm> files) {
    return files.stream()
        .map(UploadedFileForm::getFileId)
        .toList();
  }

  private Map<WebUserAccountId, EnergyPortalUserJson> getUploaders(Collection<UploadedFile> files) {
    var wuaIds = files.stream()
        .map(UploadedFile::getUploadedBy)
        .filter(Objects::nonNull)
        .map(wuaId -> WebUserAccountId.from(Long.valueOf(wuaId)))
        .collect(Collectors.toSet());
    return wuaIds.isEmpty()
        ? Map.of()
        : energyPortalUserService.getEnergyPortalUserMap(wuaIds, UPLOADER_USER_PURPOSE);
  }

  private static String getUploaderName(UploadedFile file, Map<WebUserAccountId, EnergyPortalUserJson> uploaders) {
    return Optional.ofNullable(file.getUploadedBy())
        .map(wuaId -> uploaders.get(WebUserAccountId.from(Long.valueOf(wuaId))))
        .map(EnergyPortalUserJson::displayName)
        .orElse("Unknown user");
  }
}
