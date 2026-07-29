package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class RecordOfDecisionSectionService implements TaskListSectionService<RecordOfDecisionTaskListContext> {

  static final String SECTION_NAME = "Record of decision";
  static final int SECTION_ORDER = 10;
  static final String WHAT_IS_THE_DECISION = "What is the decision?";
  static final String EXTENSION_DECISION_DETAILS = "Extension decision details";
  static final String CORRESPONDING_REDUCTION_DETAILS = "Corresponding reduction details";
  static final String WORK_PROGRAMME_AMENDMENT_DETAILS = "Work programme amendment details";

  private static final String URL = "#";

  private final RecordOfDecisionService recordOfDecisionService;

  public RecordOfDecisionSectionService(RecordOfDecisionService recordOfDecisionService) {
    this.recordOfDecisionService = recordOfDecisionService;
  }

  @Override
  public Optional<TaskListSection> getSection(RecordOfDecisionTaskListContext context, ServiceUserDetail user) {
    var applicationDetail = context.applicationDetail();
    var items = new ArrayList<TaskListItem>();

    boolean whatIsTheDecisionComplete = recordOfDecisionService.findByApplicationDetail(applicationDetail)
        .map(recordOfDecision ->
            recordOfDecision.getExtensionDecision() != null && recordOfDecision.getWorkProgrammeDecision() != null)
        .orElse(false);

    // TODO LMS1-542: step records the decision and sets this task's completion
    items.add(new TaskListItem(
        WHAT_IS_THE_DECISION, TaskListLabel.notStartedOrComplete(whatIsTheDecisionComplete), URL));

    if (recordOfDecisionService.isExtensionApproved(applicationDetail)) {
      // TODO LMS1-543: extension details step sets visibility and completion
      items.add(new TaskListItem(EXTENSION_DECISION_DETAILS, TaskListLabel.notStartedOrComplete(false), URL));
    }

    if (recordOfDecisionService.isExtensionDetailsSaved(applicationDetail)) {
      // TODO LMS1-544: corresponding reduction details step sets visibility and completion
      items.add(new TaskListItem(CORRESPONDING_REDUCTION_DETAILS, TaskListLabel.notStartedOrComplete(false), URL));
    }

    if (recordOfDecisionService.isWorkProgrammeAmendmentApproved(applicationDetail)) {
      // TODO LMS1-545/546: work programme amendment step sets visibility and completion
      items.add(new TaskListItem(WORK_PROGRAMME_AMENDMENT_DETAILS, TaskListLabel.notStartedOrComplete(false), URL));
    }

    return Optional.of(new TaskListSection(SECTION_NAME, SECTION_ORDER, items));
  }
}
