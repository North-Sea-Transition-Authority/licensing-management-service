package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

class MigrationValidationReportTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(2019, Month.APRIL, 1);

  private static final LivePositionComparison.UnmatchedPositions MATCHED =
      new LivePositionComparison.UnmatchedPositions(0, 0);
  private static final LivePositionComparison.UnmatchedPositions ONE_MISSING =
      new LivePositionComparison.UnmatchedPositions(1, 0);

  private final MigrationValidationReport report = new MigrationValidationReport();

  @Test
  void render_whenEveryLicenceMatchesBothReadingsOfPears_thenSaysSoForEach() {
    sweep();
    sweep();
    report.recordComparison(PearsComparisonSource.OPERATIONS, "P1", positions("REF-A"), 1, MATCHED, List.of());
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P1", positions("REF-A"), 1, MATCHED, List.of());
    report.recordComparison(PearsComparisonSource.OPERATIONS, "P2", positions("REF-B"), 1, MATCHED, List.of());
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P2", positions("REF-B"), 1, MATCHED, List.of());

    var rendered = report.render();

    assertThat(rendered)
        .contains("2 licences swept")
        .contains("--- Against the operation-derived positions (checks the replay) ---")
        .contains("--- Against the PEARS data points (checks the reading) ---")
        .contains("Every licence compared holds the positions PEARS holds, on the same dates, in the same order.");
    assertThat(report.differingLicenceReferences(PearsComparisonSource.OPERATIONS)).isEmpty();
    assertThat(report.differingLicenceReferences(PearsComparisonSource.DATA_POINTS)).isEmpty();
  }

  @Test
  void render_whenALicenceMatchesTheOperationsButNotTheDataPoints_thenOnlyTheDataPointsReportItAsDiffering() {
    sweep();
    report.recordComparison(PearsComparisonSource.OPERATIONS, "P1", positions("REF-A"), 1, MATCHED, List.of());
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P1", positions("REF-A"), 1, ONE_MISSING, List.of(
        new PositionDifference(PositionDifference.Kind.POSITIONS, "1 position(s) PEARS holds and this build does not")
    ));

    var rendered = report.render();

    assertThat(rendered)
        .contains("differs: positions")
        .contains("e.g. P1 -- 1 position(s) PEARS holds and this build does not");
    assertThat(report.differingLicenceReferences(PearsComparisonSource.OPERATIONS)).isEmpty();
    assertThat(report.differingLicenceReferences(PearsComparisonSource.DATA_POINTS)).containsExactly("P1");
  }

  @Test
  void render_whenALicenceDiffersInSeveralWays_thenItIsBucketedByItsCoarsestDifference() {
    sweep();
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P1", positions("REF-A"), 1, ONE_MISSING, List.of(
        new PositionDifference(PositionDifference.Kind.POSITION_ORDER, "REF-A is #1 here and #2 in PEARS"),
        new PositionDifference(PositionDifference.Kind.POSITIONS, "1 position(s) PEARS holds and this build does not")
    ));

    var rendered = report.render();

    assertThat(rendered)
        .contains("differs: positions")
        .contains("position order")
        .doesNotContain("differs: position order");
  }

  @Test
  void render_whenLicencesWereNotComparedOrFailed_thenBothAreReported() {
    report.recordNotCompared("PEDL5", "PEARS licence type PL maps to LANDWARD_PRODUCTION, whose prefix is PEDL");
    report.recordFailure("P9", new IllegalStateException("Could not read live positions for P9\nsecond line"));

    var rendered = report.render();

    assertThat(rendered)
        .contains("Licences not compared:")
        .contains("PEARS licence type PL maps to LANDWARD_PRODUCTION, whose prefix is PEDL")
        .contains("PEDL5")
        .contains("Licences that could not be compared at all:")
        .contains("IllegalStateException: Could not read live positions for P9")
        .doesNotContain("second line");
  }

  @Test
  void differingLicenceReferences_whenSeveralLicencesDiffer_thenTheyAreSorted() {
    var difference = new PositionDifference(PositionDifference.Kind.POSITION_DATES, "a date differs");
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P3", positions("REF-C"), 1, MATCHED, List.of(difference));
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P1", positions("REF-A"), 1, MATCHED, List.of(difference));
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P2", positions("REF-B"), 1, MATCHED, List.of());

    assertThat(report.differingLicenceReferences(PearsComparisonSource.DATA_POINTS)).containsExactly("P1", "P3");
  }

  @Test
  void render_whenAMigratorTookDecisionsAndCouldNotCarrySomethingAcross_thenBothAreTallied() {
    sweep();
    var notes = new MigrationNotes();
    notes.note("licence administrator REMOVE dropped, because administrator state is single valued");
    notes.unmapped("licence administrator entry with no joining organisation", 49336);

    report.recordMigratorNotes("licence administrator", "P1", notes);

    var rendered = report.render();

    assertThat(rendered)
        .contains("--- The licence administrator, carried across ---")
        .contains("1 licences migrated")
        .contains("Licences by what the migration decided on them:")
        .contains("licence administrator REMOVE dropped, because administrator state is single valued")
        .contains("Licences by what PEARS holds that could not be carried across:")
        .contains("e.g. P1 -- PEARS operation 49336");
  }

  @Test
  void render_whenPositionsAreMissingOrAdditional_thenTheyAreTotalledPerReadingOfPears() {
    sweep();
    sweep();
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P1", positions("REF-A"), 1,
        new LivePositionComparison.UnmatchedPositions(2, 1), List.of());
    report.recordComparison(PearsComparisonSource.DATA_POINTS, "P2", positions("REF-B"), 1,
        new LivePositionComparison.UnmatchedPositions(1, 0), List.of());
    report.recordComparison(PearsComparisonSource.OPERATIONS, "P1", positions("REF-A"), 1, MATCHED, List.of());

    var rendered = report.render();

    assertThat(rendered)
        .contains("3 positions PEARS holds that are missing from this application")
        .contains("1 positions this application holds that PEARS does not")
        .contains("0 positions PEARS holds that are missing from this application")
        .contains("0 positions this application holds that PEARS does not");
  }

  @Test
  void render_whenOperationsWereIgnored_thenTheyAreCountedInTotalAndByType() {
    sweep();
    sweep();
    report.recordIgnoredOperations("P1", Map.of("PED_BLOCK_CREATE", 3, "LICENCE_END", 1));
    report.recordIgnoredOperations("P2", Map.of("PED_BLOCK_CREATE", 2));

    var rendered = report.render();

    assertThat(rendered)
        .contains("--- Operations ignored, because no migration supports them ---")
        .contains("6 operations ignored on 2 licences")
        .containsPattern("PED_BLOCK_CREATE +5 operations on 2 licences")
        .containsPattern("LICENCE_END +1 operations on 1 licences");
  }

  @Test
  void render_whenOperationsWereIncluded_thenTheyAreCountedInTotalAndByType() {
    sweep();
    sweep();
    sweep();
    report.recordIncludedOperations("P1", Map.of("CONSORTIUM_LIST_CREATE", 1, "CONSORTIUM_LIST_CHANGE", 2));
    report.recordIncludedOperations("P2", Map.of("CONSORTIUM_LIST_CREATE", 1));
    report.recordIncludedOperations("P3", Map.of());

    var rendered = report.render();

    assertThat(rendered)
        .contains("--- Operations included, because a migration supports them ---")
        .contains("4 operations included on 2 licences")
        .containsPattern("CONSORTIUM_LIST_CHANGE +2 operations on 1 licences")
        .containsPattern("CONSORTIUM_LIST_CREATE +2 operations on 2 licences");
  }

  @Test
  void render_whenNoOperationsWereIncludedOrIgnored_thenBothCountsAreStillShown() {
    sweep();

    var rendered = report.render();

    assertThat(rendered)
        .contains("0 operations included on 0 licences")
        .contains("0 operations ignored on 0 licences");
  }

  private void sweep() {
    report.recordLicenceSwept();
  }

  private static PearsLicencePositions positions(String regulatorReference) {
    return new PearsLicencePositions("P", 1, List.of(
        new PearsLicencePositions.Position(POSITION_DATE, 1, 1, regulatorReference, 0)
    ));
  }
}
