package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision.RecordFinalDecisionFileUsage;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;

@ExtendWith(MockitoExtension.class)
class RecordOfDecisionTaskListServiceTest {

  private static final Long SUBMITTED_BY_WUA_ID = 100L;

  @Mock
  private ApplicationFileService applicationFileService;

  @Mock
  private LicenceService licenceService;

  @Mock
  private EnergyPortalUserService energyPortalUserService;

  @Mock
  private TaskListSectionService<RecordOfDecisionTaskListContext> firstSectionService;

  @Mock
  private TaskListSectionService<RecordOfDecisionTaskListContext> secondSectionService;

  private RecordOfDecisionTaskListService recordOfDecisionTaskListService;
  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    recordOfDecisionTaskListService = new RecordOfDecisionTaskListService(
        List.of(firstSectionService, secondSectionService),
        applicationFileService,
        licenceService,
        energyPortalUserService);
  }

  @Test
  void getTaskListSections_sortsSectionsByDisplayOrder() {
    var context = new RecordOfDecisionTaskListContext(applicationDetail);
    var user = ServiceUserDetailTestUtil.newBuilder().build();
    when(firstSectionService.getSection(context, user))
        .thenReturn(Optional.of(new TaskListSection("Review", 20, List.of())));
    when(secondSectionService.getSection(context, user))
        .thenReturn(Optional.of(new TaskListSection("Record of decision", 10, List.of())));

    var sections = recordOfDecisionTaskListService.getTaskListSections(context, user);

    assertThat(sections)
        .extracting(TaskListSection::displayName)
        .containsExactly("Record of decision", "Review");
  }

  @Test
  void getSignedDspSummaryItem_whenNoFiles_returnsEmpty() {
    when(applicationFileService.getUploadedFiles(RecordFinalDecisionFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of());

    assertThat(recordOfDecisionTaskListService.getSignedDspSummaryItem(applicationDetail)).isEmpty();
  }

  @Test
  void getSignedDspSummaryItem_whenFilesExist_returnsSummaryItem() {
    var uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setName("signed-dsp.pdf");
    when(applicationFileService.getUploadedFiles(RecordFinalDecisionFileUsage.fromApplication(applicationDetail)))
        .thenReturn(List.of(uploadedFile));

    assertThat(recordOfDecisionTaskListService.getSignedDspSummaryItem(applicationDetail)).isPresent();
  }

  @Test
  void getApplicationContext_returnsReferenceAndType() {
    var applicationDetailWithContext = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .withSubmittedDatetime(Instant.parse("2024-03-15T10:30:00Z"))
        .withSubmittedByWuaId(SUBMITTED_BY_WUA_ID)
        .withApplicationReference("LMS/EAA/2024/1")
        .build();
    var licence = applicationDetailWithContext.getLicence();

    var submittedByUser = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(SUBMITTED_BY_WUA_ID)
        .withForename("John")
        .withSurname("Smith")
        .buildJson();

    when(energyPortalUserService.getByWuaId(
        WebUserAccountId.from(SUBMITTED_BY_WUA_ID), RecordOfDecisionTaskListService.SUBMITTED_BY_USER_PURPOSE))
        .thenReturn(submittedByUser);
    when(licenceService.getLicencePageCaption(licence)).thenReturn("Carbon storage licence - CS1");

    var context = recordOfDecisionTaskListService.getApplicationContext(applicationDetailWithContext);

    assertThat(context.reference()).isEqualTo("LMS/EAA/2024/1");
    assertThat(context.type()).isEqualTo("Carbon storage licence - CS1");
  }
}
