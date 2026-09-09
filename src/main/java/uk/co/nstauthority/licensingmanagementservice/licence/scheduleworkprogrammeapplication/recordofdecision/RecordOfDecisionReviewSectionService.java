package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision.review.ReviewRecordOfDecisionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class RecordOfDecisionReviewSectionService implements TaskListSectionService<RecordOfDecisionTaskListContext> {

  static final String SECTION_NAME = "Review";
  static final int SECTION_ORDER = 20;
  static final String REVIEW_RECORD_OF_DECISION = "Review record of decision";

  @Override
  public Optional<TaskListSection> getSection(RecordOfDecisionTaskListContext context, ServiceUserDetail user) {
    var items = List.of(new TaskListItem(
        REVIEW_RECORD_OF_DECISION,
        ReverseRouter.route(on(ReviewRecordOfDecisionController.class).renderReview(
            context.applicationDetail().getId(),
            null,
            null))));

    return Optional.of(new TaskListSection(SECTION_NAME, SECTION_ORDER, items));
  }
}
