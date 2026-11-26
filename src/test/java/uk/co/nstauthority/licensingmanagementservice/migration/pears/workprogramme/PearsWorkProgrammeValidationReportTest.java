package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.CommentRow;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeComparison.Row;

class PearsWorkProgrammeValidationReportTest {

  @Test
  void render_countsRowsAndListsOutcomesIssuesAndActivitiesNotMigrated() {
    var report = new PearsWorkProgrammeValidationReport();
    report.recordLicenceSwept();
    report.recordLicenceSwept();
    var notes = new MigrationNotes();
    notes.note("General note: to the schedule comments area");
    report.recordOutcome("P2244", new PearsWorkProgrammeMigrationOutcome(
        new PearsLicenceReference("P", 2244), PearsWorkProgrammeMigrationOutcome.Result.MIGRATED, null, notes));
    report.recordScheduleNotMigrated("P251", "SKIPPED: No terms");
    report.recordComparison(new PearsWorkProgrammeComparison("P2244", null, null,
        List.of(
            row("701", Result.MATCH, null),
            row("702", Result.NOT_MIGRATED, "No activity"),
            row("703", Result.DIFFERS, "Description differs")
        ),
        List.of(new CommentRow("A note", null, Result.MISSING_IN_LMS))));

    var text = report.render();

    assertThat(text)
        .contains("2 licences swept")
        .contains("Match")
        .contains("Description differs")
        .contains("No activity")
        .contains("Schedule comment Missing in LMS")
        .contains("General note: to the schedule comments area")
        .contains("SKIPPED: No terms");
  }

  private static Row row(String activityId, Result result, String detail) {
    return new Row(activityId, "OTHER", null, "Studies", "NOT_STARTED", null, null, null, null, null, null, result,
        detail);
  }
}
