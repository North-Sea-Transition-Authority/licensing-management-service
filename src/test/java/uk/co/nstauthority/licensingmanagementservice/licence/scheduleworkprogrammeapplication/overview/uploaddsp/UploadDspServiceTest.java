package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.file.FileUploadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class UploadDspServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository detailRepository;

  @Mock
  private ApplicationFileService applicationFileService;

  @Mock
  private ApplicationLetterService applicationLetterService;

  @InjectMocks
  private UploadDspService service;

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
}
