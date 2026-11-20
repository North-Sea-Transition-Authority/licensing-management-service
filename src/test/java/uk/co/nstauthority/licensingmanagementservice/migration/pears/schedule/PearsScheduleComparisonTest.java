package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Row;

class PearsScheduleComparisonTest {

  @Test
  void hasIssues_whenEveryRowMatchesOrIsLeftOutOnPurpose_isFalse() {
    assertThat(comparison(null, Result.MATCH, Result.CARRIED_ON, Result.NOT_MIGRATED, Result.NO_EXPIRY).hasIssues())
        .isFalse();
  }

  @Test
  void hasIssues_whenARowShowsAnIssue_isTrue() {
    assertThat(comparison(null, Result.MATCH, Result.MISSING_IN_LMS).hasIssues()).isTrue();
  }

  @Test
  void hasIssues_whenTheMigrationFailsTheLicence_isTrue() {
    assertThat(comparison("No terms in PEARS", Result.NOT_MIGRATED, Result.NO_EXPIRY).hasIssues()).isTrue();
  }

  @Test
  void hasIssues_whenTheLicenceCouldNotBeCompared_isTrue() {
    assertThat(PearsScheduleComparison.problem("P9999", "Not a licence in LMS").hasIssues()).isTrue();
  }

  private static PearsScheduleComparison comparison(String migrationFailure, Result... results) {
    var rows = List.of(results).stream()
        .map(result -> new Row("Rate", "Caption", 1, null, null, null, null, null, null, result, null))
        .toList();
    return new PearsScheduleComparison("P1556", null, migrationFailure, List.of(
        new PearsScheduleComparison.Version(1L, null, null, null, null, null, null, rows)));
  }
}
