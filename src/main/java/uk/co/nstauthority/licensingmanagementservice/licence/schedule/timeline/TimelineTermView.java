package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

public record TimelineTermView(
    List<ScheduleEvent> events,
    TermType termType,
    String dateDurationString,
    String endDateString,
    String updateUrl,
    String deleteUrl
) {
}