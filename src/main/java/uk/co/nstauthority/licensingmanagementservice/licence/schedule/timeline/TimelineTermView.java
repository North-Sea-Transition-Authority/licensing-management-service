package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentView;

public record TimelineTermView(
    List<ScheduleEvent> events,
    List<ScheduleEvent> endOfTermEvents,
    TermType termType,
    String dateDurationString,
    String endDateString,
    String updateUrl,
    String deleteUrl,
    String addCommentUrl,
    boolean hasPhases,
    List<EventCommentView> comments
) {
}