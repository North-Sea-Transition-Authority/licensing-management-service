package uk.co.nstauthority.template.xyzapplication.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.tasklist.TaskListItem;
import uk.co.nstauthority.template.tasklist.TaskListLabel;
import uk.co.nstauthority.template.tasklist.TaskListSection;
import uk.co.nstauthority.template.tasklist.TaskListSectionService;
import uk.co.nstauthority.template.workarea.WorkAreaController;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;
import uk.co.nstauthority.template.xyzapplication.form.XyzApplicationFormController;

@Service
public class XyzApplicationTaskListSectionService implements TaskListSectionService<XyzApplication> {

  static final String APPLICATION_DETAILS_SECTION_NAME = "XyzApplication details";
  static final String APPLICATION_DETAILS_ITEM_NAME = "XyzApplication details";
  static final String OTHER_DETAILS_ITEM_NAME = "Other details";
  static final int SECTION_ORDER = 10;

  @Override
  public Optional<TaskListSection> getSection(XyzApplication xyzApplication, ServiceUserDetail user) {
    var items = List.of(
        new TaskListItem(
            APPLICATION_DETAILS_ITEM_NAME,
            TaskListLabel.notStartedOrComplete(false),
            ReverseRouter.route(on(XyzApplicationFormController.class).getForm(xyzApplication.getId(), null, null))
        ),
        new TaskListItem(
            OTHER_DETAILS_ITEM_NAME,
            TaskListLabel.notStartedOrComplete(false),
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
        )
    );
    return Optional.of(new TaskListSection(APPLICATION_DETAILS_SECTION_NAME, SECTION_ORDER, items));
  }
}
