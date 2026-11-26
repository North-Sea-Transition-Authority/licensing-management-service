package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.List;

/**
 * A work programme's activities mapped for LMS, in the order PEARS holds them, and those left out.
 *
 * @param skipped the activities not migrated, each with the reason, for the migration report
 */
public record MappedActivities(List<MappedActivity> activities, List<SkippedActivity> skipped) {

  public MappedActivities {
    activities = List.copyOf(activities);
    skipped = List.copyOf(skipped);
  }

  public record SkippedActivity(PearsActivity source, String reason) {
  }
}
