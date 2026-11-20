package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspCorrectedPayload;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.DspUploadedPayload;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileTestUtil;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class UploadDspServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository detailRepository;

  @Mock
  private ApplicationFileService applicationFileService;

  @Mock
  private ApplicationLetterService applicationLetterService;

  @Mock
  private CaseEventService caseEventService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @InjectMocks
  private UploadDspService service;

  @BeforeEach
  void setUp() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void getFormForApplication_whenDecisionDateSet_populatesDate() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    applicationDetail.setDecisionDate(LocalDate.of(2024, Month.MARCH, 15));

    when(applicationFileService.getUploadedFiles(UploadDspFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getDecisionDate().getAsLocalDate()).isEqualTo(Optional.of(LocalDate.of(2024, Month.MARCH, 15)));
  }

  @Test
  void getFormForApplication_whenDecisionDateNull_leavesDateBlank() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();

    when(applicationFileService.getUploadedFiles(UploadDspFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getDecisionDate().getAsLocalDate()).isEmpty();
  }

  @Test
  void getFormForApplication_returnsFilesFromApplicationFileService() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();

    when(applicationFileService.getUploadedFiles(UploadDspFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getFinalDecisionSupportPapers()).isEmpty();
  }

  @Test
  void uploadDspDateStatusAndSaves() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    var form = new UploadDspForm();
    form.getDecisionDate().setDate(LocalDate.of(2024, Month.MARCH, 15));

    service.uploadDsp(applicationDetail, form);

    assertThat(applicationDetail.getDecisionDate()).isEqualTo(LocalDate.of(2024, Month.MARCH, 15));
    assertThat(applicationDetail.getStatus()).isEqualTo(ApplicationStatus.DSP_UPLOADED);
    verify(applicationLetterService).createDocumentInstance(applicationDetail.getScheduleWorkProgrammeApplication());
    verify(detailRepository).save(applicationDetail);
  }

  @Test
  void uploadDsp_savesDocuments() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    var form = new UploadDspForm();
    form.getDecisionDate().setDate(LocalDate.of(2024, Month.MARCH, 15));
    var papers = new ArrayList<UploadedFileForm>(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("decision.pdf", "Final decision paper")));
    form.setFinalDecisionSupportPapers(papers);

    service.uploadDsp(applicationDetail, form);

    verify(applicationFileService).saveDocuments(
        UploadDspFileUsage.fromApplication(applicationDetail),
        papers
    );
  }

  @Test
  void uploadDsp_recordsCaseEventWithUploadedFiles() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    var form = new UploadDspForm();
    form.getDecisionDate().setDate(LocalDate.of(2024, Month.MARCH, 15));
    form.setFinalDecisionSupportPapers(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("decision.pdf", "Final decision paper")));

    service.uploadDsp(applicationDetail, form);

    verify(caseEventService).recordCaseEvent(
        CaseEventType.DSP_UPLOADED,
        applicationDetail.getScheduleWorkProgrammeApplication(),
        new DspUploadedPayload(List.of(FileUploadTestUtil.FILE_ID))
    );
  }

  @Test
  void correctDsp_replacesFilesAndRecordsCaseEvent() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withStatus(ApplicationStatus.DSP_UPLOADED)
        .build();
    var form = new UploadDspForm();
    var papers = new ArrayList<UploadedFileForm>(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("corrected.pdf", "Corrected paper")));
    form.setFinalDecisionSupportPapers(papers);
    var fileUsage = UploadDspFileUsage.fromApplication(applicationDetail);
    var supersededFileUsage = UploadDspFileUsage.supersededFromApplication(applicationDetail);
    var replacedFile = ApplicationFileTestUtil.createUploadFile();
    when(applicationFileService.moveFiles(fileUsage, supersededFileUsage)).thenReturn(List.of(replacedFile));

    service.correctDsp(applicationDetail, form);

    var inOrder = inOrder(applicationFileService, caseEventService);
    inOrder.verify(applicationFileService).moveFiles(fileUsage, supersededFileUsage);
    inOrder.verify(applicationFileService).saveDocuments(fileUsage, papers);
    inOrder.verify(caseEventService).recordCaseEvent(
        CaseEventType.DSP_CORRECTED,
        applicationDetail.getScheduleWorkProgrammeApplication(),
        new DspCorrectedPayload(List.of(replacedFile.getId()), List.of(FileUploadTestUtil.FILE_ID))
    );
    assertThat(applicationDetail.getStatus()).isEqualTo(ApplicationStatus.DSP_UPLOADED);
    verifyNoMoreInteractions(detailRepository, applicationLetterService);
  }

  @Test
  void getCurrentDsp_returnsDecisionDateFilesAndUploader() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    applicationDetail.setDecisionDate(LocalDate.of(2024, Month.MARCH, 15));
    var fileId = UUID.randomUUID();
    var uploadedAt = Instant.parse("2024-03-16T10:15:00Z");
    var file = mock(UploadedFile.class);
    when(file.getId()).thenReturn(fileId);
    when(file.getName()).thenReturn("decision.pdf");
    when(file.getUploadedBy()).thenReturn("5");
    when(file.getUploadedAt()).thenReturn(uploadedAt);
    when(applicationFileService.getUploadedFiles(UploadDspFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of(file));
    var uploader = EnergyPortalUserJson.from(EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(5L)
        .withForename("Jane")
        .withSurname("Smith")
        .build());
    when(energyPortalUserService.findByWuaId(WebUserAccountId.from(5L), UploadDspService.UPLOADER_USER_PURPOSE))
        .thenReturn(Optional.of(uploader));

    var result = service.getCurrentDsp(applicationDetail);

    var expected = new CurrentDspView(
        DateFormatUtil.convertToDisplayText(LocalDate.of(2024, Month.MARCH, 15)),
        List.of(new CurrentDspView.DspFileView(
            "decision.pdf",
            ReverseRouter.route(on(UploadDspController.class)
                .downloadFile(fileId, applicationDetail.getId(), null, null))
        )),
        "%s, %s".formatted(uploader.displayName(), DateFormatUtil.convertToDisplayTextWithTime(uploadedAt))
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
