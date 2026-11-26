package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivityHistory.Comment;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.MappedActivityHistory.Status;

class PearsNoteMapperTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  private static final LocalDateTime SAVED = LocalDateTime.of(2024, Month.MAY, 1, 9, 0);
  private static final LocalDateTime BULK_JOB = LocalDateTime.of(2011, Month.JULY, 12, 17, 47, 26);
  private static final LocalDateTime MARCH_2015 = LocalDateTime.of(2015, Month.MARCH, 2, 10, 0);
  private static final LocalDateTime JUNE_2016 = LocalDateTime.of(2016, Month.JUNE, 7, 14, 30);

  @Test
  void notes_buildTheStatusHistoryFromTheStatusNotesOldestFirst() {
    var notes = new MigrationNotes();
    var well = activity("501", "DRILL_OR_DROP_WELL", "DROPPED", JUNE_2016);

    var history = history(programme(List.of(well), List.of(
        note("501", "DROPPED", JUNE_2016, "Jen Brzozowska", "Licensee chose to drop the well"),
        note("501", "IN_PROGRESS", MARCH_2015, "Mrs Jen Brzozowska", "Drilling started")
    )), notes, "501");

    assertThat(history.statuses()).containsExactly(
        new Status(MigratedStatus.IN_PROGRESS, MARCH_2015, "IN_PROGRESS"),
        new Status(MigratedStatus.DROPPED, JUNE_2016, "DROPPED")
    );
    assertThat(history.comments()).containsExactly(
        new Comment("Drilling started (PEARS note by Mrs Jen Brzozowska)", MARCH_2015),
        new Comment("Licensee chose to drop the well (PEARS note by Jen Brzozowska)", JUNE_2016)
    );
    assertThat(notes.noteCountsByReason()).containsEntry(PearsNoteMapper.STATUS_FROM_NOTES, 1);
  }

  @Test
  void notes_whenThereIsNoStatusNote_thenOneStatusDatedFromTheFirstVersionHoldingIt() {
    var notes = new MigrationNotes();
    var seismic = activity("502", "SEISMIC", "COMPLETE", MARCH_2015);

    var history = history(programme(List.of(seismic), List.of()), notes, "502");

    assertThat(history.statuses()).containsExactly(new Status(MigratedStatus.COMPLETE, MARCH_2015, "COMPLETE"));
    assertThat(history.comments()).isEmpty();
    assertThat(notes.noteCountsByReason()).containsEntry(PearsNoteMapper.STATUS_WITHOUT_NOTE, 1);
  }

  @Test
  void notes_notStartedWithNoNoteIsOpenAndNotNoted() {
    var notes = new MigrationNotes();

    var history = history(programme(List.of(activity("503", "OTHER", "NOT_STARTED", null)), List.of()), notes, "503");

    assertThat(history.statuses()).containsExactly(new Status(MigratedStatus.OPEN, SAVED, "NOT_STARTED"));
    assertThat(notes.noteCountsByReason()).isEmpty();
  }

  @Test
  void notes_legacyStatusSetByTheBulkJobGetsItsComment() {
    var notes = new MigrationNotes();

    var history = history(programme(List.of(activity("504", "LEGACY", "COMPLETE", BULK_JOB)), List.of()), notes, "504");

    assertThat(history.statuses()).containsExactly(new Status(MigratedStatus.COMPLETE, BULK_JOB, "COMPLETE"));
    assertThat(history.comments()).containsExactly(new Comment(PearsNoteMapper.LEGACY_STATUS_COMMENT, BULK_JOB));
    assertThat(notes.noteCountsByReason()).containsEntry(PearsNoteMapper.LEGACY_STATUS_CARRIED_OVER, 1);
  }

  @Test
  void notes_statusChangedAndChangedBackKeepsEveryStep() {
    var history = history(programme(List.of(activity("505", "OTHER", "NOT_STARTED", BULK_JOB)), List.of(
        note("505", "IN_PROGRESS", MARCH_2015, "Jen Brzozowska", "Started in error"),
        note("505", "NOT_STARTED", JUNE_2016, "Jen Brzozowska", "Set back")
    )), new MigrationNotes(), "505");

    assertThat(history.statuses()).extracting(Status::status)
        .containsExactly(MigratedStatus.IN_PROGRESS, MigratedStatus.OPEN);
  }

  @Test
  void notes_whenTheCurrentStatusIsNotTheLastNotes_thenItIsAddedAfterThem() {
    var notes = new MigrationNotes();
    var activity = activity("506", "OTHER", "COMPLETE", JUNE_2016);

    var history = history(programme(List.of(activity), List.of(
        note("506", "IN_PROGRESS", MARCH_2015, "Jen Brzozowska", "Started")
    )), notes, "506");

    assertThat(history.statuses()).containsExactly(
        new Status(MigratedStatus.IN_PROGRESS, MARCH_2015, "IN_PROGRESS"),
        new Status(MigratedStatus.COMPLETE, JUNE_2016, "COMPLETE")
    );
    assertThat(notes.noteCountsByReason()).containsEntry(PearsNoteMapper.CURRENT_STATUS_AFTER_NOTES, 1);
  }

  @Test
  void notes_waiverStatusesMapProvisionallyWithAComment() {
    var notes = new MigrationNotes();

    var mapped = PearsNoteMapper.notes(programme(List.of(
        activity("507", "FIRM_WELL", "PART_WAIVER", MARCH_2015),
        activity("508", "SEISMIC", "WAIVER_DECLINED", JUNE_2016)
    ), List.of()), activities(), notes);
    var partWaiver = mapped.histories().get("507");
    var waiverDeclined = mapped.histories().get("508");

    assertThat(partWaiver.statuses()).extracting(Status::status).containsExactly(MigratedStatus.IN_PROGRESS);
    assertThat(partWaiver.comments()).extracting(Comment::text)
        .containsExactly("PEARS status: PART_WAIVER, which LMS doesn't have");
    assertThat(waiverDeclined.statuses()).extracting(Status::status).containsExactly(MigratedStatus.OPEN);
    assertThat(notes.noteCountsByReason())
        .containsKey(PearsNoteMapper.PART_WAIVER)
        .containsKey(PearsNoteMapper.WAIVER_DECLINED);
  }

  @Test
  void notes_stubGetsItsComment() {
    var stub = activity("509", "LEGACY", "NOT_STARTED", BULK_JOB);
    var programme = programme(List.of(stub), List.of());
    var mappedStub = new MappedActivity("509", null, PearsActivityMapper.LEGACY_NAME, null, "Stub activity", true, stub);

    var mapped = PearsNoteMapper.notes(programme, new MappedActivities(List.of(mappedStub), List.of()),
        new MigrationNotes());

    assertThat(mapped.histories().get("509").comments())
        .containsExactly(new Comment(PearsActivityMapper.STUB_COMMENT, BULK_JOB));
  }

  @Test
  void notes_generalNotesAndNotesOnActivitiesNotMigratedGoToTheScheduleComments() {
    var notes = new MigrationNotes();
    var programme = programme(List.of(activity("510", "OTHER", "NOT_STARTED", null)), List.of(
        note(null, null, JUNE_2016, "Jen Brzozowska", "Licence is determining with outstanding work"),
        new PearsNote(2, "DROPPED", "999", "Contingent Well 1", "DROPPED", MARCH_2015, "Jen Brzozowska", "Dropped")
    ));

    var mapped = PearsNoteMapper.notes(programme, activities(), notes);

    assertThat(mapped.scheduleComments()).containsExactly(
        new Comment("Contingent Well 1: Dropped (PEARS note by Jen Brzozowska)", MARCH_2015),
        new Comment("Licence is determining with outstanding work (PEARS note by Jen Brzozowska)", JUNE_2016)
    );
    assertThat(notes.noteCountsByReason())
        .containsEntry(PearsNoteMapper.GENERAL_NOTE, 1)
        .containsEntry(PearsNoteMapper.NOTE_ON_ACTIVITY_NOT_MIGRATED, 1);
  }

  @Test
  void notes_bulkJobNotesAreLeftOutAndCounted() {
    var notes = new MigrationNotes();
    var programme = programme(List.of(activity("511", "LEGACY", "COMPLETE", BULK_JOB)), List.of(
        note(null, null, BULK_JOB, "Migration Process", "Start Date: 01/12/2014"),
        note(null, null, BULK_JOB, "Migration Process", "[[MIGRATED DATA]] - Required to drill 1 drill or drop well(s)")
    ));

    var mapped = PearsNoteMapper.notes(programme, activities(), notes);

    assertThat(mapped.scheduleComments()).isEmpty();
    assertThat(notes.noteCountsByReason()).containsEntry(PearsNoteMapper.SYSTEM_NOTE_LEFT_OUT, 2);
  }

  @Test
  void notes_aStatusNoteWithNoTextSaysWhatChanged() {
    var history = history(programme(List.of(activity("512", "OTHER", "COMPLETE", MARCH_2015)), List.of(
        note("512", "COMPLETE", MARCH_2015, null, " ")
    )), new MigrationNotes(), "512");

    assertThat(history.comments()).extracting(Comment::text).containsExactly("Status changed to COMPLETE (PEARS note)");
  }

  @Test
  void notes_aBreakpointWithNoDrillOrDropWellIsAScheduleComment() {
    var notes = new MigrationNotes();
    var programme = new PearsWorkProgramme(P2244, 11L, 101L, SAVED, "INITIAL", null, "NOT_STARTED",
        new PearsWorkProgramme.Breakpoint("4", "1", "0"), List.of(activity("513", "FIRM_WELL", "NOT_STARTED", null)),
        List.of());

    var mapped = PearsNoteMapper.notes(programme, PearsActivityMapper.activities(programme, new MigrationNotes()), notes);

    assertThat(mapped.scheduleComments()).containsExactly(new Comment(
        "Drill-or-drop breakpoint: 4 years, 1 month from licence start (PEARS work programme)", SAVED));
    assertThat(notes.noteCountsByReason()).containsKey(PearsNoteMapper.BREAKPOINT_WITHOUT_DRILL_OR_DROP_WELL);
  }

  @Test
  void notes_whenAStatusIsUnknown_thenTheLicenceFails() {
    var programme = programme(List.of(activity("514", "OTHER", "HALF_DONE", null)), List.of());
    var activities = PearsActivityMapper.activities(programme, new MigrationNotes());
    var notes = new MigrationNotes();

    assertThatThrownBy(() -> PearsNoteMapper.notes(programme, activities, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessage("P2244: Activity 514 has the status 'HALF_DONE', which the migration doesn't know");
  }

  @Test
  void notes_whenAnActivityNotMigratedHasAnUnknownStatus_thenTheLicenceStillFails() {
    var none = new PearsActivity(1, PearsActivity.ListKind.FLAT, null, "515", "SHOOT_3D_SEISMIC", null, "HALF_DONE",
        null, "None", null);
    var programme = programme(List.of(none), List.of());
    var activities = PearsActivityMapper.activities(programme, new MigrationNotes());
    var notes = new MigrationNotes();

    assertThat(activities.activities()).isEmpty();
    assertThatThrownBy(() -> PearsNoteMapper.notes(programme, activities, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessageContaining("'HALF_DONE'");
  }

  @Test
  void notes_whenANoteOnAnActivityNotMigratedHasAnUnknownStatus_thenTheLicenceStillFails() {
    var programme = programme(List.of(activity("516", "OTHER", "NOT_STARTED", null)), List.of(
        new PearsNote(1, "HALF_DONE", "999", "Gone 1", "HALF_DONE", MARCH_2015, "Jen Brzozowska", "Odd")
    ));
    var activities = PearsActivityMapper.activities(programme, new MigrationNotes());
    var notes = new MigrationNotes();

    assertThatThrownBy(() -> PearsNoteMapper.notes(programme, activities, notes))
        .isInstanceOf(PearsWorkProgrammeMappingException.class)
        .hasMessage("P2244: Activity 999 has the status 'HALF_DONE', which the migration doesn't know");
  }

  @Test
  void notes_whenTheOnlyDrillOrDropWellIsNotMigrated_thenTheBreakpointIsAScheduleComment() {
    var noneWell = new PearsActivity(1, PearsActivity.ListKind.FLAT, null, "517", "DRILL_OR_DROP_WELL", null,
        "NOT_STARTED", null, "None", null);
    var programme = new PearsWorkProgramme(P2244, 11L, 101L, SAVED, "INITIAL", null, "NOT_STARTED",
        new PearsWorkProgramme.Breakpoint("3", null, null), List.of(noneWell), List.of());

    var mapped = PearsNoteMapper.notes(programme, PearsActivityMapper.activities(programme, new MigrationNotes()),
        new MigrationNotes());

    assertThat(mapped.scheduleComments()).extracting(Comment::text)
        .containsExactly("Drill-or-drop breakpoint: 3 years from licence start (PEARS work programme)");
  }

  /**
   * The activities of the last programme built, as {@link PearsActivityMapper} maps them.
   */
  private MappedActivities activities() {
    return PearsActivityMapper.activities(lastProgramme, new MigrationNotes());
  }

  private PearsWorkProgramme lastProgramme;

  private MappedActivityHistory history(PearsWorkProgramme programme, MigrationNotes notes, String activityId) {
    return PearsNoteMapper.notes(programme, PearsActivityMapper.activities(programme, new MigrationNotes()), notes)
        .histories().get(activityId);
  }

  private PearsWorkProgramme programme(List<PearsActivity> activities, List<PearsNote> notes) {
    lastProgramme = new PearsWorkProgramme(P2244, 11L, 101L, SAVED, "INITIAL", null, "NOT_STARTED", null, activities,
        notes);
    return lastProgramme;
  }

  private static PearsActivity activity(String activityId, String activityType, String status, LocalDateTime statusSince) {
    return new PearsActivity(Integer.parseInt(activityId), PearsActivity.ListKind.FLAT, null, activityId, activityType,
        null, status, null, "Some work", statusSince);
  }

  private static PearsNote note(String activityId, String newStatus, LocalDateTime datetime, String createdBy,
                                String description) {
    return new PearsNote(1, activityId == null ? "GENERAL" : newStatus, activityId, null, newStatus, datetime,
        createdBy, description);
  }
}
