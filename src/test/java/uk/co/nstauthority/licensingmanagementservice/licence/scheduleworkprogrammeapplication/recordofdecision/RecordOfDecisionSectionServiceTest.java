package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;

@ExtendWith(MockitoExtension.class)
class RecordOfDecisionSectionServiceTest {

  @Mock
  private RecordOfDecisionService recordOfDecisionService;

  @Mock
  private RecordReductionDetailsService recordReductionDetailsService;

  @Mock
  private RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  @InjectMocks
  private RecordOfDecisionSectionService recordOfDecisionSectionService;

  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;
  private RecordOfDecisionTaskListContext context;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
    context = new RecordOfDecisionTaskListContext(applicationDetail);
  }

  private void mockDecisions(
      Optional<RecordOfDecision> recordOfDecision,
      boolean extensionApproved,
      boolean extensionDetailsSaved,
      boolean workProgrammeAmendmentApproved) {
    when(recordOfDecisionService.findByApplicationDetail(applicationDetail)).thenReturn(recordOfDecision);
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(extensionApproved);
    when(recordOfDecisionService.isExtensionDetailsSaved(applicationDetail)).thenReturn(extensionDetailsSaved);
    when(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail))
        .thenReturn(workProgrammeAmendmentApproved);
  }

  @Test
  void getSection_returnsRecordOfDecisionSectionNameAndOrder() {
    mockDecisions(Optional.empty(), false, false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.displayName()).isEqualTo(RecordOfDecisionSectionService.SECTION_NAME);
    assertThat(section.displayOrder()).isEqualTo(RecordOfDecisionSectionService.SECTION_ORDER);
  }

  @Test
  void getSection_whenNothingApproved_showsOnlyWhatIsTheDecision() {
    mockDecisions(Optional.empty(), false, false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(RecordOfDecisionSectionService.WHAT_IS_THE_DECISION);
  }

  @Test
  void getSection_whenExtensionApproved_showsExtensionTask() {
    mockDecisions(Optional.empty(), true, false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.EXTENSION_DECISION_DETAILS);
  }

  @Test
  void getSection_whenExtensionDetailsSaved_showsCorrespondingReductionTask() {
    mockDecisions(Optional.empty(), true, true, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.EXTENSION_DECISION_DETAILS,
            RecordOfDecisionSectionService.CORRESPONDING_REDUCTION_DETAILS);
  }

  @Test
  void getSection_whenReductionBalancesTheExtension_marksCorrespondingReductionComplete() {
    mockDecisions(Optional.empty(), true, true, false);
    when(recordReductionDetailsService.isReductionComplete(applicationDetail)).thenReturn(true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(RecordOfDecisionSectionService.CORRESPONDING_REDUCTION_DETAILS))
        .extracting(TaskListItem::label)
        .containsExactly(TaskListLabel.COMPLETE);
  }

  @Test
  void getSection_whenReductionNoLongerBalancesTheExtension_marksCorrespondingReductionNotComplete() {
    mockDecisions(Optional.empty(), true, true, false);
    when(recordReductionDetailsService.isReductionComplete(applicationDetail)).thenReturn(false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(RecordOfDecisionSectionService.CORRESPONDING_REDUCTION_DETAILS))
        .extracting(TaskListItem::label)
        .containsExactly(TaskListLabel.NOT_COMPLETE);
  }

  @Test
  void getSection_whenWorkProgrammeAmendmentApproved_showsWorkProgrammeTask() {
    mockDecisions(Optional.empty(), false, false, true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS);
  }

  @Test
  void getSection_whenWorkProgrammeAmendmentRecorded_marksWorkProgrammeTaskComplete() {
    mockDecisions(Optional.empty(), false, false, true);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS))
        .extracting(TaskListItem::label)
        .containsExactly(TaskListLabel.COMPLETE);
  }

  @Test
  void getSection_whenNoWorkProgrammeAmendmentRecorded_marksWorkProgrammeTaskNotComplete() {
    mockDecisions(Optional.empty(), false, false, true);
    when(recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)).thenReturn(false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS))
        .extracting(TaskListItem::label)
        .containsExactly(TaskListLabel.NOT_COMPLETE);
  }

  @Test
  void getSection_whenWorkProgrammeAmendmentApproved_linksToSelectWorkProgrammeActivity() {
    mockDecisions(Optional.empty(), false, false, true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS))
        .extracting(TaskListItem::actionUrl)
        .containsExactly(ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
            .renderForm(applicationDetail.getId(), null)));
  }

  @Test
  void getSection_whenEverythingApproved_showsTasksInAcceptanceCriteriaOrder() {
    mockDecisions(Optional.empty(), true, true, true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.EXTENSION_DECISION_DETAILS,
            RecordOfDecisionSectionService.CORRESPONDING_REDUCTION_DETAILS,
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS);
  }

  @Test
  void getSection_whenDecisionAnswered_marksWhatIsTheDecisionComplete() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setExtensionDecision(RecordOfDecisionResponse.REJECTED);
    recordOfDecision.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);
    mockDecisions(Optional.of(recordOfDecision), false, false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items().getFirst().label()).isEqualTo(TaskListLabel.COMPLETE);
  }

  @Test
  void getSection_whenDecisionNotAnswered_marksWhatIsTheDecisionNotComplete() {
    mockDecisions(Optional.empty(), false, false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items().getFirst().label()).isEqualTo(TaskListLabel.NOT_COMPLETE);
  }
}
