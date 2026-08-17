package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;

@Service
public class PartialSurrenderReviewAndSubmitTaskListSectionService
    implements TaskListSectionService<PartialSurrenderTaskListContext> {

  static final String REVIEW_AND_SUBMIT = "Review and submit";
  static final int SECTION_ORDER = 999;

  @Override
  public Optional<TaskListSection> getSection(PartialSurrenderTaskListContext context, ServiceUserDetail user) {
    var positionCorrection = context.positionCorrection();

    var items = List.of(new TaskListItem(
        REVIEW_AND_SUBMIT,
        ReverseRouter.route(on(PartialSurrenderTaskListController.class).renderReviewAndSubmit(
            positionCorrection.getLicenceCorrection().getId(),
            positionCorrection.getId(),
            null,
            null
        ))
    ));

    return Optional.of(new TaskListSection(REVIEW_AND_SUBMIT, SECTION_ORDER, items));
  }
}
