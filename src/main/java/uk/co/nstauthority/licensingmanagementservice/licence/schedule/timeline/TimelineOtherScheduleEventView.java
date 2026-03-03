package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;

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

    return new TimelineOtherScheduleEventView(
        otherScheduleEvent.getCategoryString(),
        otherScheduleEvent.getDescription(),
        otherScheduleEvent.getEventDate(),
        eventDateString,
        "",
        ""
    );
  }

}
