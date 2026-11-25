package uk.co.nstauthority.licensingmanagementservice.caseevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.nstauthority.licensingmanagementservice.audit.AuditRevisionUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseAllocationPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseNotePayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateRequestPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateSubmittedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspAddendumUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspCorrectedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileTestUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.summary.UploadedFileView;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class CaseEventServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-24T10:15:30Z");
  private static final UUID APPLICATION_ID = UUID.randomUUID();
  private static final CaseNotePayload PAYLOAD = new CaseNotePayload(UUID.randomUUID());

  @Mock
  private CaseEventRepository caseEventRepository;

  @Mock
  private UserDetailService userDetailService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private FileService fileService;

  @Captor
  private ArgumentCaptor<CaseEvent> caseEventCaptor;

  private CaseEventService caseEventService;

  private ScheduleWorkProgrammeApplication application;

  @BeforeEach
  void setUp() {
    caseEventService = new CaseEventService(
        caseEventRepository,
        userDetailService,
        energyPortalUserService,
        fileService,
        Clock.fixed(NOW, ZoneId.of("UTC"))
    );

    application = new ScheduleWorkProgrammeApplication();
    application.setId(APPLICATION_ID);
  }

  @Test
  void recordCaseEvent_whenUserLoggedIn_thenRecordsUserAndProxyUser() {
    var user = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(10L)
        .withProxyWuaId(20L)
        .build();
    when(userDetailService.findUserDetail()).thenReturn(Optional.of(user));

    caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, PAYLOAD);

    verify(caseEventRepository).save(caseEventCaptor.capture());
    assertThat(caseEventCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedCaseEvent(new CaseEventUser(10L, 20L, false)));
  }

  @Test
  void recordCaseEvent_whenNoUserLoggedInButFallbackAuditUserSet_thenRecordsFallbackUser() {
    var fallbackUser = ServiceUserDetailTestUtil.newBuilder()
        .withWuaId(30L)
        .buildWithoutProxy();
    when(userDetailService.findUserDetail()).thenReturn(Optional.empty());

    AuditRevisionUtil.withFallbackAuditUser(
        fallbackUser,
        () -> caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, PAYLOAD)
    );

    verify(caseEventRepository).save(caseEventCaptor.capture());
    assertThat(caseEventCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedCaseEvent(new CaseEventUser(30L, null, false)));
  }

  @Test
  void recordCaseEvent_whenNoUserAtAll_thenRecordsSystemEvent() {
    when(userDetailService.findUserDetail()).thenReturn(Optional.empty());

    caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, PAYLOAD);

    verify(caseEventRepository).save(caseEventCaptor.capture());
    assertThat(caseEventCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedCaseEvent(new CaseEventUser(null, null, true)));
  }

  @Test
  void getCaseEventViews_whenNoEvents_thenReturnsNoViews() {
    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of());

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).isEmpty();
    verifyNoInteractions(energyPortalUserService);
  }

  @Test
  void getCaseEventViews_whenEvents_thenOneViewPerEventShowingProxyOrUserOrSystem() {
    var proxyEventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var userEventInstant = Instant.parse("2026-09-23T12:00:00Z");
    var systemEventInstant = Instant.parse("2026-09-22T12:00:00Z");

    var proxyEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, proxyEventInstant, new CaseEventUser(10L, 20L, false), null);
    var userEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, userEventInstant, new CaseEventUser(10L, null, false), null);
    var systemEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, systemEventInstant, CaseEventUser.system(), null);

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(proxyEvent, userEvent, systemEvent));

    var proxyUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(20L)
        .withForename("Proxy")
        .withSurname("User")
        .withEmailAddress("proxy.user@example.com")
        .buildJson();
    var realUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(10L)
        .withForename("Real")
        .withSurname("User")
        .withEmailAddress("real.user@example.com")
        .buildJson();
    when(energyPortalUserService.getEnergyPortalUserMap(
        List.of(WebUserAccountId.from(20L), WebUserAccountId.from(10L)),
        CaseEventService.CASE_EVENT_USER_PURPOSE))
        .thenReturn(Map.of(
            WebUserAccountId.from(20L), proxyUser,
            WebUserAccountId.from(10L), realUser
        ));

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.APPLICATION_SUBMITTED, Map.of(), "Proxy User (proxy.user@example.com)",
            proxyEventInstant),
        new CaseEventView(
            CaseEventType.APPLICATION_SUBMITTED, Map.of(), "Real User (real.user@example.com)",
            userEventInstant),
        new CaseEventView(
            CaseEventType.APPLICATION_SUBMITTED, Map.of(), CaseEventService.SYSTEM_USER_DISPLAY_NAME,
            systemEventInstant)
    );
  }

  @Test
  void getCaseEventViews_whenUpdateRequestedEvent_thenShowsDeadlineAndReason() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var updateRequestedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_UPDATE_REQUESTED, eventInstant, new CaseEventUser(10L, null, false),
        new CaseUpdateRequestPayload(LocalDate.of(2026, Month.DECEMBER, 1), "Further information is needed"));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(updateRequestedEvent));

    var caseManager = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(10L)
        .withForename("Case")
        .withSurname("Manager")
        .withEmailAddress("case.manager@example.com")
        .buildJson();
    when(energyPortalUserService.getEnergyPortalUserMap(
        List.of(WebUserAccountId.from(10L)),
        CaseEventService.CASE_EVENT_USER_PURPOSE))
        .thenReturn(Map.of(WebUserAccountId.from(10L), caseManager));

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.APPLICATION_UPDATE_REQUESTED,
            Map.of(
                CaseEventService.UPDATE_REASON_KEY, "Further information is needed",
                CaseEventService.UPDATE_DEADLINE_KEY, DateFormatUtil.convertToDisplayText(LocalDate.of(2026, Month.DECEMBER, 1))
            ),
            "Case Manager (case.manager@example.com)",
            eventInstant
        )
    );
  }

  @Test
  void getCaseEventViews_whenUpdateSubmittedEvent_thenShowsVersion() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var updateSubmittedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_UPDATE_SUBMITTED, eventInstant, new CaseEventUser(10L, null, false),
        new CaseUpdateSubmittedPayload(2));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(updateSubmittedEvent));

    var submitter = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(10L)
        .withForename("Industry")
        .withSurname("User")
        .withEmailAddress("industry.user@example.com")
        .buildJson();
    when(energyPortalUserService.getEnergyPortalUserMap(
        List.of(WebUserAccountId.from(10L)),
        CaseEventService.CASE_EVENT_USER_PURPOSE))
        .thenReturn(Map.of(WebUserAccountId.from(10L), submitter));

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.APPLICATION_UPDATE_SUBMITTED,
            Map.of(CaseEventService.VERSION_KEY, "2"),
            "Industry User (industry.user@example.com)",
            eventInstant
        )
    );
  }

  @Test
  void getCaseEventViews_whenAllocationEvent_thenShowsAllocatedToUser() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var allocationEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.STEWARD_ALLOCATED, eventInstant, new CaseEventUser(10L, null, false),
        new CaseAllocationPayload(30L));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(allocationEvent));

    var caseManager = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(10L)
        .withForename("Case")
        .withSurname("Manager")
        .withEmailAddress("case.manager@example.com")
        .buildJson();
    var steward = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(30L)
        .withForename("John")
        .withSurname("Steward")
        .withEmailAddress("john.steward@example.com")
        .buildJson();
    when(energyPortalUserService.getEnergyPortalUserMap(
        List.of(WebUserAccountId.from(10L), WebUserAccountId.from(30L)),
        CaseEventService.CASE_EVENT_USER_PURPOSE))
        .thenReturn(Map.of(
            WebUserAccountId.from(10L), caseManager,
            WebUserAccountId.from(30L), steward
        ));

    var result = caseEventService.getCaseEventViews(application);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.STEWARD_ALLOCATED,
            Map.of(CaseEventService.ALLOCATED_TO_KEY, "John Steward (john.steward@example.com)"),
            "Case Manager (case.manager@example.com)",
            eventInstant
        )
    );
  }

  @Test
  void getCaseEventViews_whenDspUploadedEvent_thenShowsDspFilesWithDownloadLinks() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var file = ApplicationFileTestUtil.createUploadFile();
    var dspUploadedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_UPLOADED, eventInstant, new CaseEventUser(10L, null, false),
        new DspUploadedPayload(List.of(file.getId())));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(dspUploadedEvent));
    stubCaseManagerUser();
    when(fileService.findAll(List.of(file.getId()))).thenReturn(List.of(file));

    var result = caseEventService.getCaseEventViews(application, fileId -> "/download/" + fileId);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.DSP_UPLOADED,
            Map.of(),
            Map.of(CaseEventService.DSP_KEY, List.of(UploadedFileView.from(file, "/download/" + file.getId()))),
            "Case Manager (case.manager@example.com)",
            eventInstant
        )
    );
  }

  @Test
  void getCaseEventViews_whenSeveralEventsHaveFiles_thenResolvesAllFilesInOneCall() {
    var firstFile = ApplicationFileTestUtil.createUploadFile();
    var secondFile = ApplicationFileTestUtil.createUploadFile();
    var firstEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_UPLOADED, Instant.parse("2026-09-24T12:00:00Z"), new CaseEventUser(10L, null, false),
        new DspUploadedPayload(List.of(firstFile.getId())));
    var secondEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_UPLOADED, Instant.parse("2026-09-23T12:00:00Z"), new CaseEventUser(10L, null, false),
        new DspUploadedPayload(List.of(secondFile.getId())));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(firstEvent, secondEvent));
    stubCaseManagerUser();
    when(fileService.findAll(List.of(firstFile.getId(), secondFile.getId())))
        .thenReturn(List.of(firstFile, secondFile));

    var result = caseEventService.getCaseEventViews(application, fileId -> "/download/" + fileId);

    assertThat(result).extracting(CaseEventView::files).containsExactly(
        Map.of(CaseEventService.DSP_KEY, List.of(UploadedFileView.from(firstFile, "/download/" + firstFile.getId()))),
        Map.of(CaseEventService.DSP_KEY, List.of(UploadedFileView.from(secondFile, "/download/" + secondFile.getId())))
    );
    verify(fileService).findAll(List.of(firstFile.getId(), secondFile.getId()));
  }

  @Test
  void getCaseEventViews_whenDspFileNoLongerExists_thenShowsNoFiles() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var missingFileId = UUID.randomUUID();
    var dspUploadedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_UPLOADED, eventInstant, new CaseEventUser(10L, null, false),
        new DspUploadedPayload(List.of(missingFileId)));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(dspUploadedEvent));
    stubCaseManagerUser();
    when(fileService.findAll(List.of(missingFileId))).thenReturn(List.of());

    var result = caseEventService.getCaseEventViews(application, fileId -> "/download/" + fileId);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.DSP_UPLOADED,
            Map.of(),
            "Case Manager (case.manager@example.com)",
            eventInstant
        )
    );
  }

  @Test
  void isFileOnApplicationEvent_whenAnEventOnTheApplicationReferencesTheFile_thenTrue() {
    var fileId = UUID.randomUUID();
    var dspUploadedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_UPLOADED, Instant.parse("2026-09-24T12:00:00Z"), new CaseEventUser(10L, null, false),
        new DspUploadedPayload(List.of(UUID.randomUUID(), fileId)));
    var submittedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, Instant.parse("2026-09-23T12:00:00Z"), new CaseEventUser(10L, null, false),
        null);

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(dspUploadedEvent, submittedEvent));

    assertThat(caseEventService.isFileOnApplicationEvent(fileId, application)).isTrue();
  }

  @Test
  void isFileOnApplicationEvent_whenNoEventOnTheApplicationReferencesTheFile_thenFalse() {
    var dspUploadedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_UPLOADED, Instant.parse("2026-09-24T12:00:00Z"), new CaseEventUser(10L, null, false),
        new DspUploadedPayload(List.of(UUID.randomUUID())));
    var submittedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.APPLICATION_SUBMITTED, Instant.parse("2026-09-23T12:00:00Z"), new CaseEventUser(10L, null, false),
        null);

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(dspUploadedEvent, submittedEvent));

    assertThat(caseEventService.isFileOnApplicationEvent(UUID.randomUUID(), application)).isFalse();
  }

  @Test
  void getCaseEventViews_whenDspCorrectedEvent_thenShowsReplacedThenCorrectedFiles() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var replacedFile = ApplicationFileTestUtil.createUploadFile();
    var correctedFile = ApplicationFileTestUtil.createUploadFile();
    var dspCorrectedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_CORRECTED, eventInstant, new CaseEventUser(10L, null, false),
        new DspCorrectedPayload(List.of(replacedFile.getId()), List.of(correctedFile.getId())));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(dspCorrectedEvent));
    stubCaseManagerUser();
    when(fileService.findAll(List.of(replacedFile.getId(), correctedFile.getId())))
        .thenReturn(List.of(correctedFile, replacedFile));

    var result = caseEventService.getCaseEventViews(application, fileId -> "/download/" + fileId);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.DSP_CORRECTED,
            Map.of(),
            Map.of(
                CaseEventService.REPLACED_DSP_KEY,
                List.of(UploadedFileView.from(replacedFile, "/download/" + replacedFile.getId())),
                CaseEventService.CORRECTED_DSP_KEY,
                List.of(UploadedFileView.from(correctedFile, "/download/" + correctedFile.getId()))
            ),
            "Case Manager (case.manager@example.com)",
            eventInstant
        )
    );
    assertThat(result.get(0).files().keySet())
        .containsExactly(CaseEventService.REPLACED_DSP_KEY, CaseEventService.CORRECTED_DSP_KEY);
  }

  @Test
  void getCaseEventViews_whenDspAddendumUploadedEvent_thenShowsAddendumFile() {
    var eventInstant = Instant.parse("2026-09-24T12:00:00Z");
    var addendumFile = ApplicationFileTestUtil.createUploadFile();
    var addendumEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_ADDENDUM_UPLOADED, eventInstant, new CaseEventUser(10L, null, false),
        new DspAddendumUploadedPayload(List.of(addendumFile.getId())));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(addendumEvent));
    stubCaseManagerUser();
    when(fileService.findAll(List.of(addendumFile.getId()))).thenReturn(List.of(addendumFile));

    var result = caseEventService.getCaseEventViews(application, fileId -> "/download/" + fileId);

    assertThat(result).containsExactly(
        new CaseEventView(
            CaseEventType.DSP_ADDENDUM_UPLOADED,
            Map.of(),
            Map.of(
                CaseEventService.DSP_ADDENDUM_KEY,
                List.of(UploadedFileView.from(addendumFile, "/download/" + addendumFile.getId()))
            ),
            "Case Manager (case.manager@example.com)",
            eventInstant
        )
    );
  }

  @Test
  void isFileOnApplicationEvent_whenTheFileWasReplacedByACorrection_thenTrue() {
    var replacedFileId = UUID.randomUUID();
    var dspCorrectedEvent = new CaseEvent(APPLICATION_ID,
        CaseEventType.DSP_CORRECTED, Instant.parse("2026-09-24T12:00:00Z"), new CaseEventUser(10L, null, false),
        new DspCorrectedPayload(List.of(replacedFileId), List.of(UUID.randomUUID())));

    when(caseEventRepository.findByApplicationIdOrderByEventInstantDesc(APPLICATION_ID))
        .thenReturn(List.of(dspCorrectedEvent));

    assertThat(caseEventService.isFileOnApplicationEvent(replacedFileId, application)).isTrue();
  }

  private void stubCaseManagerUser() {
    var caseManager = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(10L)
        .withForename("Case")
        .withSurname("Manager")
        .withEmailAddress("case.manager@example.com")
        .buildJson();
    when(energyPortalUserService.getEnergyPortalUserMap(
        List.of(WebUserAccountId.from(10L)),
        CaseEventService.CASE_EVENT_USER_PURPOSE))
        .thenReturn(Map.of(WebUserAccountId.from(10L), caseManager));
  }

  private CaseEvent expectedCaseEvent(CaseEventUser user) {
    return new CaseEvent(
        APPLICATION_ID,
        CaseEventType.CASE_NOTE_ADDED,
        NOW,
        user,
        PAYLOAD
    );
  }
}
