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
  private RecordDurationChangesService recordDurationChangesService;

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
      boolean workProgrammeAmendmentApproved) {
    when(recordOfDecisionService.findByApplicationDetail(applicationDetail)).thenReturn(recordOfDecision);
    when(recordOfDecisionService.isExtensionApproved(applicationDetail)).thenReturn(extensionApproved);
    when(recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail))
        .thenReturn(workProgrammeAmendmentApproved);
  }

  @Test
  void getSection_returnsRecordOfDecisionSectionNameAndOrder() {
    mockDecisions(Optional.empty(), false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.displayName()).isEqualTo(RecordOfDecisionSectionService.SECTION_NAME);
    assertThat(section.displayOrder()).isEqualTo(RecordOfDecisionSectionService.SECTION_ORDER);
  }

  @Test
  void getSection_whenNothingApproved_showsOnlyWhatIsTheDecision() {
    mockDecisions(Optional.empty(), false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(RecordOfDecisionSectionService.WHAT_IS_THE_DECISION);
  }

  @Test
  void getSection_whenExtensionApproved_showsDurationsTask() {
    mockDecisions(Optional.empty(), true, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.TERM_AND_PHASE_DURATIONS);
  }

  @Test
  void getSection_whenReductionBalancesTheExtension_marksDurationsTaskComplete() {
    mockDecisions(Optional.empty(), true, false);
    when(recordDurationChangesService.isComplete(applicationDetail)).thenReturn(true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(RecordOfDecisionSectionService.TERM_AND_PHASE_DURATIONS))
        .extracting(TaskListItem::label)
        .containsExactly(TaskListLabel.COMPLETE);
  }

  @Test
  void getSection_whenReductionDoesNotBalanceTheExtension_marksDurationsTaskNotComplete() {
    mockDecisions(Optional.empty(), true, false);
    when(recordDurationChangesService.isComplete(applicationDetail)).thenReturn(false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .filteredOn(item -> item.displayName().equals(RecordOfDecisionSectionService.TERM_AND_PHASE_DURATIONS))
        .extracting(TaskListItem::label)
        .containsExactly(TaskListLabel.NOT_COMPLETE);
  }

  @Test
  void getSection_whenWorkProgrammeAmendmentApproved_showsWorkProgrammeTask() {
    mockDecisions(Optional.empty(), false, true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS);
  }

  @Test
  void getSection_whenWorkProgrammeAmendmentRecorded_marksWorkProgrammeTaskComplete() {
    mockDecisions(Optional.empty(), false, true);
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
    mockDecisions(Optional.empty(), false, true);
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
    mockDecisions(Optional.empty(), false, true);

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
    mockDecisions(Optional.empty(), true, true);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items())
        .extracting(TaskListItem::displayName)
        .containsExactly(
            RecordOfDecisionSectionService.WHAT_IS_THE_DECISION,
            RecordOfDecisionSectionService.TERM_AND_PHASE_DURATIONS,
            RecordOfDecisionSectionService.WORK_PROGRAMME_AMENDMENT_DETAILS);
  }

  @Test
  void getSection_whenDecisionAnswered_marksWhatIsTheDecisionComplete() {
    var recordOfDecision = new RecordOfDecision();
    recordOfDecision.setExtensionDecision(RecordOfDecisionResponse.REJECTED);
    recordOfDecision.setWorkProgrammeDecision(RecordOfDecisionResponse.NOT_REQUESTED);
    mockDecisions(Optional.of(recordOfDecision), false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items().getFirst().label()).isEqualTo(TaskListLabel.COMPLETE);
  }

  @Test
  void getSection_whenDecisionNotAnswered_marksWhatIsTheDecisionNotComplete() {
    mockDecisions(Optional.empty(), false, false);

    var section = recordOfDecisionSectionService.getSection(context, user).orElseThrow();

    assertThat(section.items().getFirst().label()).isEqualTo(TaskListLabel.NOT_COMPLETE);
  }
}
