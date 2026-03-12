package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;

@ExtendWith(MockitoExtension.class)
class RecordFinalDecisionServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationDetailRepository detailRepository;

  @Mock
  private ApplicationFileService applicationFileService;

  @InjectMocks
  private RecordFinalDecisionService service;

  @Test
  void getFormForApplication_whenDecisionDateSet_populatesDate() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    applicationDetail.setDecisionDate(LocalDate.of(2024, 3, 15));

    when(applicationFileService.getUploadedFiles(RecordFinalDecisionFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getDecisionDate().getAsLocalDate()).isEqualTo(Optional.of(LocalDate.of(2024, 3, 15)));
  }

  @Test
  void getFormForApplication_whenDecisionDateNull_leavesDateBlank() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();

    when(applicationFileService.getUploadedFiles(RecordFinalDecisionFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getDecisionDate().getAsLocalDate()).isEqualTo(Optional.empty());
  }

  @Test
  void getFormForApplication_returnsFilesFromApplicationFileService() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();

    when(applicationFileService.getUploadedFiles(RecordFinalDecisionFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getFinalDecisionSupportPapers()).isEmpty();
  }

  @Test
  void recordDecision_setsDecisionDateStatusAndSaves() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    var form = new RecordFinalDecisionForm();
    form.getDecisionDate().setDate(LocalDate.of(2024, 3, 15));

    service.recordDecision(applicationDetail, form);

    assertThat(applicationDetail.getDecisionDate()).isEqualTo(LocalDate.of(2024, 3, 15));
    assertThat(applicationDetail.getStatus()).isEqualTo(ScheduleWorkProgrammeApplicationStatus.ISSUE_DECISION);
    verify(detailRepository).save(applicationDetail);
  }

  @Test
  void recordDecision_savesDocuments() {
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    var form = new RecordFinalDecisionForm();
    form.getDecisionDate().setDate(LocalDate.of(2024, 3, 15));
    var papers = new ArrayList<UploadedFileForm>(List.of(
        FileUploadTestUtil.getUploadedFileFormWithDescription("decision.pdf", "Final decision paper")));
    form.setFinalDecisionSupportPapers(papers);

    service.recordDecision(applicationDetail, form);

    verify(applicationFileService).saveDocuments(
        RecordFinalDecisionFileUsage.fromApplication(applicationDetail),
        papers
    );
  }
}
