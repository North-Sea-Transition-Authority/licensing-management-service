package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDeletionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineOtherScheduleEventView(
    String category,
    String description,
    LocalDate eventDate,
    String eventDateString,
    String updateUrl,
    String deleteUrl,
    String addCommentUrl,
    List<EventCommentView> comments
) implements ScheduleEvent {

  @Override
  public ScheduleEventType getEventType() {
    return ScheduleEventType.OTHER;
  }

  @Override
  public LocalDate getSortingDate() {
    return eventDate;
  }

  public static ScheduleEvent getScheduleEventFrom(
      OtherScheduleEvent otherScheduleEvent,
      List<ScheduleEventAction> allowedActions,
      Map<UUID, List<EventCommentView>> eventComments
  ) {
    var eventDateString = otherScheduleEvent.getEventDate() != null
        ? DateFormatUtil.convertToDisplayText(otherScheduleEvent.getEventDate())
        : "";

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(OtherScheduleEventController.class)
        .renderUpdateEventForm(otherScheduleEvent.getId()))
        : "";

    var deleteUrl = allowedActions.contains(ScheduleEventAction.EDIT_SCHEDULE_EVENTS)
        ? ReverseRouter.route(on(OtherScheduleEventDeletionController.class)
          .renderDeleteEventPage(otherScheduleEvent.getId()))
        : "";

    var addCommentUrl = allowedActions.contains(ScheduleEventAction.ADD_SCHEDULE_COMMENT)
        ? ReverseRouter.route(on(EventCommentController.class)
          .renderAddCommentForm(otherScheduleEvent.getEventReference().getId(), null))
        : "";

    var comments = eventComments.getOrDefault(otherScheduleEvent.getEventReference().getId(), List.of());

    return new TimelineOtherScheduleEventView(
        otherScheduleEvent.getCategoryString(),
        otherScheduleEvent.getDescription(),
        otherScheduleEvent.getEventDate(),
        eventDateString,
        editUrl,
        deleteUrl,
        addCommentUrl,
        comments
    );
  }

}
