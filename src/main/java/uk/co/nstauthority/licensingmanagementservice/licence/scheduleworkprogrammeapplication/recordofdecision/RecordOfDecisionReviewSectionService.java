package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class RecordOfDecisionReviewSectionService implements TaskListSectionService<RecordOfDecisionTaskListContext> {

  static final String SECTION_NAME = "Review";
  static final int SECTION_ORDER = 20;
  static final String REVIEW_RECORD_OF_DECISION = "Review record of decision";

  private static final String URL = "#";

  @Override
  public Optional<TaskListSection> getSection(RecordOfDecisionTaskListContext context, ServiceUserDetail user) {
    // TODO LMS1-548: review step sets the link and completion
    var items = List.of(new TaskListItem(
        REVIEW_RECORD_OF_DECISION,
        TaskListLabel.notStartedOrComplete(false),
        URL));

    return Optional.of(new TaskListSection(SECTION_NAME, SECTION_ORDER, items));
  }
}
