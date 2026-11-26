package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivities.SkippedActivity;

/**
 * A licence's PEARS work programmes mapped to what its LMS schedule's {@code ACTIVE} detail will hold, ready to be
 * written.
 *
 * @param activities       every migrated activity, across the licence's programmes, in PEARS order
 * @param skipped          the activities not migrated, with the reason, for the report
 * @param scheduleComments the comments for the schedule comments area, oldest first
 * @param notes            what the mapper decided along the way, counted by reason, for the report
 */
public record MappedWorkProgramme(
    PearsLicenceReference licence,
    List<Activity> activities,
    List<SkippedActivity> skipped,
    List<MappedActivityHistory.Comment> scheduleComments,
    MigrationNotes notes
) {

  public MappedWorkProgramme {
    activities = List.copyOf(activities);
    skipped = List.copyOf(skipped);
    scheduleComments = List.copyOf(scheduleComments);
  }

  /**
   * One migrated activity: what it holds, where it sits on the schedule, and its status history and comments.
   *
   * @param pedWpId  the PEARS programme it is on
   * @param pedWpdId the programme version it was read from
   */
  public record Activity(
      long pedWpId,
      long pedWpdId,
      MappedActivity activity,
      MappedActivityDate date,
      MappedActivityHistory history
  ) {
  }
}
