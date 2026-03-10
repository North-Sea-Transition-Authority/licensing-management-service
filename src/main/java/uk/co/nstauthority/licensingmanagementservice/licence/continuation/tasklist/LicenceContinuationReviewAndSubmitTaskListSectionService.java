package uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit.ContinuationApplicationReviewAndSubmitController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class LicenceContinuationReviewAndSubmitTaskListSectionService
    implements TaskListSectionService<LicenceContinuationApplicationDetail> {

  static final String REVIEW_AND_SUBMIT = "Review and submit";
  static final int SECTION_ORDER = 30;

  @Override
  public Optional<TaskListSection> getSection(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var items = new ArrayList<>(List.of(new TaskListItem(
        REVIEW_AND_SUBMIT,
        ReverseRouter.route(on(ContinuationApplicationReviewAndSubmitController.class).getReviewAndSubmit(
            licenceContinuationApplicationDetail.getId(),
            null
        ))
    )));

    return Optional.of(new TaskListSection(
        REVIEW_AND_SUBMIT,
        SECTION_ORDER,
        items
    ));
  }
}