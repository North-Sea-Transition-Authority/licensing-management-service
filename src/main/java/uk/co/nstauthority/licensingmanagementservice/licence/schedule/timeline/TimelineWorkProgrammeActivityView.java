package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDeletionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineWorkProgrammeActivityView(
    String category,
    String description,
    LocalDate dueDate,
    String dueDateString,
    String updateUrl,
    String deleteUrl
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.WORK_PROGRAMME_ACTIVITY;
  }

  @Override
  public LocalDate getSortingDate() {
    return dueDate;
  }

  public static ScheduleEvent getScheduleEventFrom(WorkProgrammeActivity workProgrammeActivity) {
    var dueDateString = workProgrammeActivity.getDueDate() != null
        ? DateFormatUtil.convertToDisplayText(workProgrammeActivity.getDueDate())
        : "";

    return new TimelineWorkProgrammeActivityView(
        workProgrammeActivity.getCategoryString(),
        workProgrammeActivity.getDescription(),
        workProgrammeActivity.getDueDate(),
        dueDateString,
        ReverseRouter.route(on(WorkProgrammeActivityController.class)
            .renderUpdateActivityForm(workProgrammeActivity.getId(), null)),
        ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
            .renderDeleteActivityPage(workProgrammeActivity.getId(), null))
    );
  }

}
