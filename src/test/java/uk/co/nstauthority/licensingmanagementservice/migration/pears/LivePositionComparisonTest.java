package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

class LivePositionComparisonTest {

  private static final LocalDate FIRST_DATE = LocalDate.of(2019, Month.APRIL, 1);
  private static final LocalDate SECOND_DATE = LocalDate.of(2020, Month.JUNE, 30);
  private static final LocalDate THIRD_DATE = LocalDate.of(2021, Month.SEPTEMBER, 1);

  @Test
  void compare_whenTimelinesMatch_thenNoDifferences() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(FIRST_DATE, 9, 2, "REF-B"),
        livePosition(SECOND_DATE, 3, 1, "REF-C")
    );
    var built = List.of(
        builtPosition(FIRST_DATE, 1, "REF-A"),
        builtPosition(FIRST_DATE, 2, "REF-B"),
        builtPosition(SECOND_DATE, 1, "REF-C")
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).isEmpty();
  }

  @Test
  void compare_whenBuiltPositionsAreOutOfChronologicalOrder_thenTheyAreSortedBeforeComparing() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(SECOND_DATE, 3, 1, "REF-B")
    );
    var built = List.of(
        builtPosition(SECOND_DATE, 1, "REF-B"),
        builtPosition(FIRST_DATE, 1, "REF-A")
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).isEmpty();
  }

  @Test
  void compare_whenPearsHoldsAPositionThatWasNotBuilt_thenPositionsDiffer() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(SECOND_DATE, 3, 1, "REF-B")
    );
    var built = List.of(builtPosition(FIRST_DATE, 1, "REF-A"));

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITIONS,
        "1 position(s) PEARS holds and this build does not: REF-B (2020-06-30 #1)"
    ));
  }

  @Test
  void compare_whenAPositionWasBuiltThatPearsDoesNotHold_thenPositionsDiffer() {
    var livePositions = livePositions(livePosition(FIRST_DATE, 6, 1, "REF-A"));
    var built = List.of(
        builtPosition(FIRST_DATE, 1, "REF-A"),
        builtPosition(SECOND_DATE, 1, "REF-B")
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITIONS,
        "1 position(s) built that PEARS does not hold: REF-B (2020-06-30 #1)"
    ));
  }

  @Test
  void compare_whenAPositionSitsOnADifferentDate_thenPositionDatesDiffer() {
    var livePositions = livePositions(livePosition(FIRST_DATE, 6, 1, "REF-A"));
    var built = List.of(builtPosition(SECOND_DATE, 1, "REF-A"));

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITION_DATES,
        "REF-A sits on 2020-06-30 here and on 2019-04-01 in PEARS"
    ));
  }

  @Test
  void compare_whenTwoPositionsOnADateAreSwapped_thenOnlyTheFirstDivergenceIsReported() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(FIRST_DATE, 9, 2, "REF-B")
    );
    var built = List.of(
        builtPosition(FIRST_DATE, 1, "REF-B"),
        builtPosition(FIRST_DATE, 2, "REF-A")
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITION_ORDER,
        "REF-A is #2 on 2019-04-01 here and #1 in PEARS (sequence 6), and 1 more out of order"
    ));
  }

  @Test
  void compare_whenAPositionIsOrderedDifferentlyWithinItsDate_thenPositionOrderDiffers() {
    var livePositions = livePositions(livePosition(FIRST_DATE, 9, 2, "REF-A"));
    var built = List.of(builtPosition(FIRST_DATE, 1, "REF-A"));

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITION_ORDER,
        "REF-A is #1 on 2019-04-01 here and #2 in PEARS (sequence 9)"
    ));
  }

  @Test
  void compare_whenOneReferenceIsHeldOnTwoDates_thenOccurrencesArePairedInOrder() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(SECOND_DATE, 3, 1, "REF-A")
    );
    var built = List.of(
        builtPosition(FIRST_DATE, 1, "REF-A"),
        builtPosition(SECOND_DATE, 1, "REF-A")
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).isEmpty();
  }

  @Test
  void compare_whenARepeatedReferenceIsBuiltOnItsEarlierDateOnly_thenTheLaterOccurrenceIsMissing() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(SECOND_DATE, 3, 1, "REF-A")
    );
    var built = List.of(builtPosition(FIRST_DATE, 1, "REF-A"));

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITIONS,
        "1 position(s) PEARS holds and this build does not: REF-A (2020-06-30 #1)"
    ));
  }

  @Test
  void compare_whenARepeatedReferenceIsBuiltOnItsLaterDateOnly_thenTheEarlierOccurrenceIsMissing() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(SECOND_DATE, 3, 1, "REF-A")
    );
    var built = List.of(builtPosition(SECOND_DATE, 1, "REF-A"));

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITIONS,
        "1 position(s) PEARS holds and this build does not: REF-A (2019-04-01 #1)"
    ));
  }

  @Test
  void compare_whenOneOccurrenceOfARepeatedReferenceSitsOnADifferentDate_thenOnlyThatOccurrenceDiffers() {
    var livePositions = livePositions(
        livePosition(FIRST_DATE, 6, 1, "REF-A"),
        livePosition(THIRD_DATE, 4, 1, "REF-A")
    );
    var built = List.of(
        builtPosition(FIRST_DATE, 1, "REF-A"),
        builtPosition(SECOND_DATE, 1, "REF-A")
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.POSITION_DATES,
        "REF-A sits on 2020-06-30 here and on 2021-09-01 in PEARS"
    ));
  }

  @Test
  void compare_whenAPositionWasBuiltWithoutARegulatorReference_thenItIsReportedAsUnmatchable() {
    var livePositions = livePositions(livePosition(FIRST_DATE, 6, 1, "REF-A"));
    var built = List.of(
        builtPosition(FIRST_DATE, 1, "REF-A"),
        LicencePositionTestUtil.newBuilder()
            .withPositionDate(SECOND_DATE)
            .withPositionOrder(1)
            .withLicenceTransaction(null)
            .build()
    );

    var differences = LivePositionComparison.compare(livePositions, built);

    assertThat(differences).containsExactly(new PositionDifference(
        PositionDifference.Kind.REGULATOR_REFERENCES,
        "1 position(s) built without a regulator reference, so they cannot be matched to PEARS: "
            + "(no reference) (2020-06-30 #1)"
    ));
  }

  @Test
  void compare_whenMorePositionsAreMissingThanAreWorthNaming_thenTheRestBecomeACount() {
    var live = new ArrayList<PearsLicencePositions.Position>();
    for (var day = 1; day <= 15; day++) {
      live.add(livePosition(FIRST_DATE.plusDays(day), day, 1, "REF-%02d".formatted(day)));
    }

    var differences = LivePositionComparison.compare(livePositions(live), List.of());

    assertThat(differences).hasSize(1);
    assertThat(differences.getFirst().detail())
        .startsWith("15 position(s) PEARS holds and this build does not: REF-01 (2019-04-02 #1)")
        .endsWith("... (+3 more)");
  }

  private static PearsLicencePositions livePositions(PearsLicencePositions.Position... positions) {
    return livePositions(List.of(positions));
  }

  private static PearsLicencePositions livePositions(List<PearsLicencePositions.Position> positions) {
    return new PearsLicencePositions("P", 1, positions);
  }

  private static PearsLicencePositions.Position livePosition(
      LocalDate positionDate,
      int positionSequence,
      int positionDateOrder,
      String regulatorReference
  ) {
    return new PearsLicencePositions.Position(positionDate, positionSequence, positionDateOrder, regulatorReference);
  }

  private static LicencePosition builtPosition(
      LocalDate positionDate,
      int positionDateOrder,
      String regulatorReference
  ) {
    return LicencePositionTestUtil.newBuilder()
        .withPositionDate(positionDate)
        .withPositionOrder(positionDateOrder)
        .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder()
            .withRegulatorReference(regulatorReference)
            .build())
        .build();
  }
}
