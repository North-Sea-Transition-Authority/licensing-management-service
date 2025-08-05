package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListItem;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListLabel;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSection;
import uk.co.nstauthority.licensingmanagementservice.tasklist.TaskListSectionService;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Service
public class ScheduleWorkProgrammeApplicationTaskListSectionService
    implements TaskListSectionService<ScheduleWorkProgrammeApplicationDetail> {

  static final String APPLICATION_DETAILS_SECTION_NAME = "Schedule and work programme application details";
  static final String WHAT_ARE_YOU_REQUESTING_TO_DO = "What are you requesting to do?";
  static final int SECTION_ORDER = 10;

  @Override
  public Optional<TaskListSection> getSection(ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
                                              ServiceUserDetail user) {
    var items = List.of(
        new TaskListItem(
            WHAT_ARE_YOU_REQUESTING_TO_DO,
            TaskListLabel.notStartedOrComplete(false),
            ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null))
        )
    );
    return Optional.of(new TaskListSection(APPLICATION_DETAILS_SECTION_NAME, SECTION_ORDER, items));
  }
}
