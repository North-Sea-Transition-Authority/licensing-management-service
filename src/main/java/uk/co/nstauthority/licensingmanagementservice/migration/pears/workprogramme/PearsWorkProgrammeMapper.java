package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivities.SkippedActivity;

/**
 * Maps a licence's PEARS work programmes onto its migrated schedule: each programme's activities
 * ({@link PearsActivityMapper}), their dates ({@link PearsActivityDateMapper}) and their statuses and notes
 * ({@link PearsNoteMapper}). Pure code, with no database, as the schedule migration's mapper is.
 */
public final class PearsWorkProgrammeMapper {

  /**
   * A licence with two INITIAL programmes isn't migrated until the licensing team says which is right (provisional,
   * WP-L2).
   */
  public static final String TWO_INITIAL_PROGRAMMES = "Two INITIAL work programmes (provisional, WP-L2)";

  private PearsWorkProgrammeMapper() {
  }

  /**
   * The licence's work programmes, mapped.
   *
   * @param workProgrammes the licence's current programmes, as {@link PearsWorkProgrammeService} reads them
   * @param schedule       the licence's schedule, as the schedule migration wrote it
   * @throws PearsWorkProgrammeMappingException if the licence can't be migrated as it is; the reason says why
   */
  public static MappedWorkProgramme map(
      PearsLicenceReference licence,
      List<PearsWorkProgramme> workProgrammes,
      MigratedSchedule schedule
  ) {
    var initialProgrammes = workProgrammes.stream()
        .filter(workProgramme -> "INITIAL".equals(StringUtils.strip(workProgramme.type())))
        .count();
    if (initialProgrammes > 1) {
      throw new PearsWorkProgrammeMappingException(licence, TWO_INITIAL_PROGRAMMES);
    }

    var notes = new MigrationNotes();
    var activities = new ArrayList<MappedWorkProgramme.Activity>();
    var skipped = new ArrayList<SkippedActivity>();
    var scheduleComments = new ArrayList<MappedActivityHistory.Comment>();

    for (var workProgramme : workProgrammes) {
      var mappedActivities = PearsActivityMapper.activities(workProgramme, notes);
      var mappedNotes = PearsNoteMapper.notes(workProgramme, mappedActivities, notes);

      for (var activity : mappedActivities.activities()) {
        activities.add(new MappedWorkProgramme.Activity(
            workProgramme.pedWpId(),
            workProgramme.pedWpdId(),
            activity,
            PearsActivityDateMapper.date(workProgramme, activity, schedule, notes),
            mappedNotes.histories().get(activity.pearsActivityId())
        ));
      }
      skipped.addAll(mappedActivities.skipped());
      scheduleComments.addAll(mappedNotes.scheduleComments());
    }

    scheduleComments.sort(Comparator.comparing(MappedActivityHistory.Comment::timestamp));
    return new MappedWorkProgramme(licence, activities, skipped, scheduleComments, notes);
  }
}
