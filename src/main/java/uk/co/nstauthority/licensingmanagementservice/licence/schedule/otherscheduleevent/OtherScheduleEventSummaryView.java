package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

public record OtherScheduleEventSummaryView(
    String description,
    String eventDate,
    String comments
) {

  public static OtherScheduleEventSummaryView fromOtherScheduleEvent(OtherScheduleEvent otherScheduleEvent) {
    var eventDateString = otherScheduleEvent.getEventDate() != null
        ? DateFormatUtil.convertToDisplayText(otherScheduleEvent.getEventDate())
        : "";

    return new OtherScheduleEventSummaryView(
        otherScheduleEvent.getDescription(),
        eventDateString,
        otherScheduleEvent.getComments()
    );
  }

}
