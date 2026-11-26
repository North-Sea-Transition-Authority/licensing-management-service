package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.List;
import java.util.Map;

/**
 * A work programme's statuses and notes, mapped for LMS.
 *
 * @param histories        each migrated activity's status history and comments, by PEARS {@code ACTIVITY_ID}
 * @param scheduleComments the comments for the schedule comments area: general notes, notes on an activity that isn't
 *                         migrated, and a breakpoint with no drill-or-drop well
 */
public record MappedNotes(Map<String, MappedActivityHistory> histories, List<MappedActivityHistory.Comment> scheduleComments) {

  public MappedNotes {
    histories = Map.copyOf(histories);
    scheduleComments = List.copyOf(scheduleComments);
  }
}
