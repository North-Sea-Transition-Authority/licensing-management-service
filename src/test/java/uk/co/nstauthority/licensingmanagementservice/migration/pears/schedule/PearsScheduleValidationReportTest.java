package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Row;

class PearsScheduleValidationReportTest {

  private static final LocalDate DATE = LocalDate.of(2018, Month.DECEMBER, 1);

  @Test
  void render_countsOutcomesResultsIssuesAndReasonsAcrossLicences() {
    var report = new PearsScheduleValidationReport();
    var notes = new MigrationNotes();
    notes.note("Rate outside the schedule's terms: not migrated");

    report.recordLicenceSwept();
    report.recordOutcome("P1556", outcome("P", 1556, PearsScheduleMigrationOutcome.Result.MIGRATED, null, notes));
    report.recordComparison(comparison("P1556",
        row(Result.MATCH, null), row(Result.MATCH, null), row(Result.DIFFERS, null), row(Result.DIFFERS, null)));
    report.recordLicenceSwept();
    report.recordOutcome("PL211", outcome("PL", 211, PearsScheduleMigrationOutcome.Result.MIGRATED, null, notes));
    report.recordComparison(comparison("PL211",
        row(Result.MATCH, null), row(Result.NOT_MIGRATED, PearsScheduleVersionComparer.LEFT_OUT_BY_RULES)));
    report.recordLicenceSwept();
    report.recordOutcome("P1972", outcome("P", 1972, PearsScheduleMigrationOutcome.Result.FAILED, "No terms in PEARS",
        new MigrationNotes()));
    report.recordLicenceSwept();
    report.recordFailure("P1", new IllegalStateException("PEARS went away\nat the second line"));

    assertThat(report.render())
        .contains("4 licences swept")
        .containsPattern("Match +3\\R")
        .containsPattern("Differs +2  \\(issue\\)")
        .containsPattern("MIGRATED +2")
        .containsPattern("FAILED: No terms in PEARS +1")
        .containsPattern("Rate outside the schedule's terms: not migrated +2")
        .contains("Issues to look into (licences with each):")
        .contains("e.g. P1556 -- version 2, Rate Caption: PEARS 2018-12-01 150, LMS 2018-12-01 175: Differs by +0 days")
        .containsPattern(PearsScheduleVersionComparer.LEFT_OUT_BY_RULES.replace("(", "\\(").replace(")", "\\)")
            .replace("'", ".") + " +1")
        .contains("IllegalStateException: PEARS went away");
  }

  @Test
  void issueDescriptions_isOneLinePerRowToLookInto() {
    var issues = comparison("P1556",
        row(Result.MATCH, null), row(Result.LMS_ONLY, "No PEARS event: added in LMS")).issueDescriptions();

    assertThat(issues).containsExactly(
        "version 2, Rate Caption: PEARS 2018-12-01 150, LMS 2018-12-01 175: LMS only (No PEARS event: added in LMS)");
  }

  private static PearsScheduleMigrationOutcome outcome(
      String licenceType,
      int licenceNo,
      PearsScheduleMigrationOutcome.Result result,
      String reason,
      MigrationNotes notes
  ) {
    return new PearsScheduleMigrationOutcome(new PearsLicenceReference(licenceType, licenceNo), result, reason, notes);
  }

  private static PearsScheduleComparison comparison(String licenceReference, Row... rows) {
    return new PearsScheduleComparison(licenceReference, null, null, List.of(new PearsScheduleComparison.Version(
        2L, null, null, null, null, null, null, List.of(rows))));
  }

  private static Row row(Result result, String detail) {
    return new Row("Rate", "Caption", 4, null, DATE, "150", null, DATE, "175", result, detail);
  }
}
