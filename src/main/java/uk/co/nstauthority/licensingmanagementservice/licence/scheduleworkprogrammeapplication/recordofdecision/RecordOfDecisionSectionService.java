package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class RecordOfDecisionSectionService implements TaskListSectionService<RecordOfDecisionTaskListContext> {

  static final String SECTION_NAME = "Record of decision";
  static final int SECTION_ORDER = 10;
  static final String WHAT_IS_THE_DECISION = "What is the decision?";
  static final String TERM_AND_PHASE_DURATIONS = "Term and phase durations";
  static final String WORK_PROGRAMME_AMENDMENT_DETAILS = "Work programme amendment details";

  private final RecordOfDecisionService recordOfDecisionService;
  private final RecordDurationChangesService recordDurationChangesService;
  private final RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService;

  public RecordOfDecisionSectionService(
      RecordOfDecisionService recordOfDecisionService,
      RecordDurationChangesService recordDurationChangesService,
      RecordWorkProgrammeAmendmentDetailsService recordWorkProgrammeAmendmentDetailsService
  ) {
    this.recordOfDecisionService = recordOfDecisionService;
    this.recordDurationChangesService = recordDurationChangesService;
    this.recordWorkProgrammeAmendmentDetailsService = recordWorkProgrammeAmendmentDetailsService;
  }

  @Override
  public Optional<TaskListSection> getSection(RecordOfDecisionTaskListContext context, ServiceUserDetail user) {
    var applicationDetail = context.applicationDetail();
    var items = new ArrayList<TaskListItem>();

    boolean whatIsTheDecisionComplete = recordOfDecisionService.findByApplicationDetail(applicationDetail)
        .map(recordOfDecision ->
            recordOfDecision.getExtensionDecision() != null && recordOfDecision.getWorkProgrammeDecision() != null)
        .orElse(false);

    items.add(new TaskListItem(
        WHAT_IS_THE_DECISION,
        TaskListLabel.notStartedOrComplete(whatIsTheDecisionComplete),
        ReverseRouter.route(on(RecordDecisionController.class).renderForm(applicationDetail.getId(), null))));

    if (recordOfDecisionService.isExtensionApproved(applicationDetail)) {
      items.add(new TaskListItem(
          TERM_AND_PHASE_DURATIONS,
          TaskListLabel.notStartedOrComplete(recordDurationChangesService.isComplete(applicationDetail)),
          ReverseRouter.route(on(RecordDurationChangesController.class).renderForm(applicationDetail.getId(), null))));
    }

    if (recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)) {
      items.add(new TaskListItem(
          WORK_PROGRAMME_AMENDMENT_DETAILS,
          TaskListLabel.notStartedOrComplete(
              recordWorkProgrammeAmendmentDetailsService.hasAmendmentDetails(applicationDetail)),
          ReverseRouter.route(on(SelectWorkProgrammeActivityController.class)
              .renderForm(applicationDetail.getId(), null))));
    }

    return Optional.of(new TaskListSection(SECTION_NAME, SECTION_ORDER, items));
  }
}
