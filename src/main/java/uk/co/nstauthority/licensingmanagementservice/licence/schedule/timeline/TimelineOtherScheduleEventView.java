package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public record TimelineOtherScheduleEventView(
    String category,
    String description,
    LocalDate eventDate,
    String eventDateString,
    String updateUrl,
    String deleteUrl
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
      List<ScheduleEventAction> allowedActions
  ) {
    var eventDateString = otherScheduleEvent.getEventDate() != null
        ? DateFormatUtil.convertToDisplayText(otherScheduleEvent.getEventDate())
        : "";

    var editUrl = allowedActions.contains(ScheduleEventAction.EDIT_WORK_PROGRAMME)
        ? ReverseRouter.route(on(OtherScheduleEventController.class)
        .renderUpdateEventForm(otherScheduleEvent.getId()))
        : "";

    return new TimelineOtherScheduleEventView(
        otherScheduleEvent.getCategoryString(),
        otherScheduleEvent.getDescription(),
        otherScheduleEvent.getEventDate(),
        eventDateString,
        editUrl,
        ""
    );
  }

}
