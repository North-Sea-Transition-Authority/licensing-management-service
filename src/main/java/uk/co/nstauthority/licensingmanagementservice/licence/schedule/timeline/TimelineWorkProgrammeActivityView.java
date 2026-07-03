package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDeletionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineWorkProgrammeActivityView(
    String category,
    String description,
    LocalDate dueDate,
    String dueDateString,
    String updateUrl,
    String deleteUrl,
    String updateStatusUrl,
    String addCommentUrl,
    WorkProgrammeStatus status,
    List<EventCommentView> comments
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.WORK_PROGRAMME_ACTIVITY;
  }

  @Override
  public LocalDate getSortingDate() {
    return dueDate;
  }

  public static ScheduleEvent getScheduleEventFrom(
      WorkProgrammeActivity workProgrammeActivity,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, WorkProgrammeActivityStatus> eventRefWorkProgrammeStatusMap,
      Map<UUID, List<EventCommentView>> eventComments
  ) {
    var dueDateString = workProgrammeActivity.getDueDate() != null
        ? DateFormatUtil.convertToDisplayText(workProgrammeActivity.getDueDate())
        : "";

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_WORK_PROGRAMME)
        ? ReverseRouter.route(on(WorkProgrammeActivityController.class)
          .renderUpdateActivityForm(workProgrammeActivity.getId(), null))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_WORK_PROGRAMME)
        ? ReverseRouter.route(on(WorkProgrammeActivityDeletionController.class)
          .renderDeleteActivityPage(workProgrammeActivity.getId(), null))
        : "";

    var editStatusUrl = allowedActions.contains(ScheduleEventAction.EDIT_WORK_PROGRAMME_STATUS)
        ? ReverseRouter.route(on(WorkProgrammeActivityStatusController.class)
          .renderStatusUpdatePage(workProgrammeActivity.getId(), null))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_WORK_PROGRAMME_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(workProgrammeActivity.getId(), null)
    )
        : "";

    var status = eventRefWorkProgrammeStatusMap.get(workProgrammeActivity.getOriginalEventId());

    var statusView = status != null
        ? status.getStatus()
        : null;

    var comments = eventComments.getOrDefault(workProgrammeActivity.getOriginalEventId(), List.of());

    return new TimelineWorkProgrammeActivityView(
        workProgrammeActivity.getCategoryString(),
        workProgrammeActivity.getDescription(),
        workProgrammeActivity.getDueDate(),
        dueDateString,
        editUrl,
        deleteUrl,
        editStatusUrl,
        addCommentUrl,
        statusView,
        comments
    );
  }

}
