package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationValidationReportTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(2019, Month.APRIL, 1);

  private final MigrationValidationReport report = new MigrationValidationReport();

  @Test
  void render_whenEveryLicenceMatchesBothReadingsOfPears_thenSaysSoForEach() {
    sweep();
    sweep();
    report.recordComparison(PearsOracle.OPERATIONS, "P1", positions("REF-A"), 1, List.of());
    report.recordComparison(PearsOracle.DATA_POINTS, "P1", positions("REF-A"), 1, List.of());
    report.recordComparison(PearsOracle.OPERATIONS, "P2", positions("REF-B"), 1, List.of());
    report.recordComparison(PearsOracle.DATA_POINTS, "P2", positions("REF-B"), 1, List.of());

    var rendered = report.render();

    assertThat(rendered)
        .contains("2 licences swept")
        .contains("--- Against the operation-derived positions (checks the replay) ---")
        .contains("--- Against the PEARS data points (checks the reading) ---")
        .contains("Every licence compared holds the positions PEARS holds, on the same dates, in the same order.");
    assertThat(report.differingLicenceReferences(PearsOracle.OPERATIONS)).isEmpty();
    assertThat(report.differingLicenceReferences(PearsOracle.DATA_POINTS)).isEmpty();
  }

  @Test
  void render_whenALicenceMatchesTheOperationsButNotTheDataPoints_thenOnlyTheDataPointsReportItAsDiffering() {
    sweep();
    report.recordComparison(PearsOracle.OPERATIONS, "P1", positions("REF-A"), 1, List.of());
    report.recordComparison(PearsOracle.DATA_POINTS, "P1", positions("REF-A"), 1, List.of(
        new PositionDifference(PositionDifference.Kind.POSITIONS, "1 position(s) PEARS holds and this build does not")
    ));

    var rendered = report.render();

    assertThat(rendered)
        .contains("differs: positions")
        .contains("e.g. P1 -- 1 position(s) PEARS holds and this build does not");
    assertThat(report.differingLicenceReferences(PearsOracle.OPERATIONS)).isEmpty();
    assertThat(report.differingLicenceReferences(PearsOracle.DATA_POINTS)).containsExactly("P1");
  }

  @Test
  void render_whenALicenceDiffersInSeveralWays_thenItIsBucketedByItsCoarsestDifference() {
    sweep();
    report.recordComparison(PearsOracle.DATA_POINTS, "P1", positions("REF-A"), 1, List.of(
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
    report.recordComparison(PearsOracle.DATA_POINTS, "P3", positions("REF-C"), 1, List.of(difference));
    report.recordComparison(PearsOracle.DATA_POINTS, "P1", positions("REF-A"), 1, List.of(difference));
    report.recordComparison(PearsOracle.DATA_POINTS, "P2", positions("REF-B"), 1, List.of());

    assertThat(report.differingLicenceReferences(PearsOracle.DATA_POINTS)).containsExactly("P1", "P3");
  }

  private void sweep() {
    report.recordLicenceSwept();
  }

  private static PearsLicencePositions positions(String regulatorReference) {
    return new PearsLicencePositions("P", 1, List.of(
        new PearsLicencePositions.Position(POSITION_DATE, 1, 1, regulatorReference)
    ));
  }
}
