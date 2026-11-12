package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionKey;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;

class PositionStateComparisonTest {

  private static final LocalDate FIRST_DATE = LocalDate.of(2019, Month.APRIL, 1);
  private static final LocalDate SECOND_DATE = LocalDate.of(2020, Month.JUNE, 30);

  private static final int BLOCK_SI_ID = 194508;
  private static final int SUBAREA_SI_ID = 194542;
  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final Map<UUID, Integer> LEGACY_IDS = Map.of(
      BLOCK_FEATURE_ID, BLOCK_SI_ID,
      SUBAREA_FEATURE_ID, SUBAREA_SI_ID
  );

  private final Map<PearsLicencePositions.PositionKey, PearsPositionState> pearsStates = new HashMap<>();
  private final Map<LivePositionComparison.MatchedPosition, LicencePositionState> lmsStatesByPosition = new HashMap<>();

  @Test
  void compare_whenEveryPositionHoldsWhatPearsHolds_thenNoDifferences() {
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsInPears(position, new PearsPositionState(
        Set.of(100),
        Set.of(100, 200),
        Set.of(BLOCK_SI_ID),
        Set.of(new PearsPositionState.Subarea(BLOCK_SI_ID, SUBAREA_SI_ID, "ALL"))
    ));
    holdsHere(position, new LicencePositionState(
        100,
        null,
        Set.of(100, 200),
        Map.of(),
        Map.of(BLOCK_FEATURE_ID, Set.of(new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea", "ALL"))),
        Map.of()
    ));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(1, 0, 0, List.of()));
  }

  @Test
  void compare_whenTheAdministratorDiffers_thenAdministratorsDiffer() {
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsInPears(position, organisations(Set.of(200), Set.of(100)));
    holdsHere(position, organisations(100, Set.of(100)));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(1, 0, 0, List.of(new PositionDifference(
        PositionDifference.Kind.ADMINISTRATORS,
        "REF-A (2019-04-01 #1) is administered by 100 here and by 200 in PEARS"
    ))));
  }

  @Test
  void compare_whenTheLicenseesDiffer_thenSaysWhatOnlyEachSideHolds() {
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsInPears(position, organisations(Set.of(100), Set.of(100, 200)));
    holdsHere(position, organisations(100, Set.of(100, 300)));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(1, 0, 0, List.of(new PositionDifference(
        PositionDifference.Kind.LICENSEES,
        "REF-A (2019-04-01 #1): only PEARS holds 200; only this application holds 300"
    ))));
  }

  @Test
  void compare_whenADifferenceCarriesOnToLaterPositions_thenOnlyTheFirstIsDescribed() {
    var first = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    var second = matchedPosition(SECOND_DATE, 3, 1, "REF-B");
    holdsInPears(first, organisations(Set.of(100), Set.of(100, 200)));
    holdsInPears(second, organisations(Set.of(100), Set.of(100, 200)));
    holdsHere(first, organisations(100, Set.of(100)));
    holdsHere(second, organisations(100, Set.of(100)));

    var result = compare(List.of(first, second));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(2, 0, 0, List.of(new PositionDifference(
        PositionDifference.Kind.LICENSEES,
        "REF-A (2019-04-01 #1): only PEARS holds 200, and at 1 later position(s)"
    ))));
  }

  @Test
  void compare_whenPearsHoldsNothingAtAPosition_thenThePositionIsNotCompared() {
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsHere(position, organisations(100, Set.of(100)));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(0, 1, 0, List.of()));
  }

  @Test
  void compare_whenABlockPearsHoldsHasNoMigratedFeature_thenItIsCountedRatherThanCompared() {
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsInPears(position, new PearsPositionState(
        Set.of(100),
        Set.of(100),
        Set.of(BLOCK_SI_ID, 999),
        Set.of(new PearsPositionState.Subarea(999, null, "ALL"))
    ));
    holdsHere(position, new LicencePositionState(100, null, Set.of(100), Map.of(), Map.of(BLOCK_FEATURE_ID, Set.of()), Map.of()));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(1, 0, 1, List.of()));
  }

  @Test
  void compare_whenBlocksAndSubareasDiffer_thenEachDiffersInItsOwnRight() {
    var otherBlockFeatureId = UUID.randomUUID();
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsInPears(position, new PearsPositionState(
        Set.of(100),
        Set.of(100),
        Set.of(BLOCK_SI_ID),
        Set.of(new PearsPositionState.Subarea(BLOCK_SI_ID, SUBAREA_SI_ID, "A"))
    ));
    holdsHere(position, new LicencePositionState(
        100,
        null,
        Set.of(100),
        Map.of(),
        Map.of(
            BLOCK_FEATURE_ID, Set.of(new SubareaDetails(null, null, "B")),
            otherBlockFeatureId, Set.of()
        ),
        Map.of()
    ));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(1, 0, 0, List.of(
        new PositionDifference(
            PositionDifference.Kind.BLOCKS,
            "REF-A (2019-04-01 #1): only this application holds feature %s".formatted(otherBlockFeatureId)
        ),
        new PositionDifference(
            PositionDifference.Kind.SUBAREAS,
            "REF-A (2019-04-01 #1): only PEARS holds SI 194508/A (SI 194542); only this application holds SI 194508/B"
        )
    )));
  }

  @Test
  void compare_whenASubareaHasNoMigratedFeature_thenItIsMatchedByItsShortName() {
    var position = matchedPosition(FIRST_DATE, 6, 1, "REF-A");
    holdsInPears(position, new PearsPositionState(
        Set.of(100),
        Set.of(100),
        Set.of(BLOCK_SI_ID),
        Set.of(new PearsPositionState.Subarea(BLOCK_SI_ID, 999, "A"))
    ));
    holdsHere(position, new LicencePositionState(
        100,
        null,
        Set.of(100),
        Map.of(),
        Map.of(BLOCK_FEATURE_ID, Set.of(new SubareaDetails(null, null, "A"))),
        Map.of()
    ));

    var result = compare(List.of(position));

    assertThat(result).isEqualTo(new PositionStateComparison.Result(1, 0, 0, List.of()));
  }

  private PositionStateComparison.Result compare(List<LivePositionComparison.MatchedPosition> matched) {
    var statesByKey = new TreeMap<PositionKey, LicencePositionState>();
    var keyByPositionId = new HashMap<UUID, PositionKey>();
    lmsStatesByPosition.forEach((position, state) -> {
      var licencePosition = position.licencePosition();
      var key = new PositionKey(licencePosition.getPositionDate(), licencePosition.getPositionDateOrder());
      statesByKey.put(key, state);
      keyByPositionId.put(licencePosition.getId(), key);
    });

    return PositionStateComparison.compare(matched, pearsStates, new ResolvedStates(statesByKey, keyByPositionId), LEGACY_IDS);
  }

  private void holdsInPears(LivePositionComparison.MatchedPosition position, PearsPositionState state) {
    pearsStates.put(position.pearsPosition().key(), state);
  }

  private void holdsHere(LivePositionComparison.MatchedPosition position, LicencePositionState state) {
    lmsStatesByPosition.put(position, state);
  }

  private static PearsPositionState organisations(Set<Integer> administratorIds, Set<Integer> licenseeIds) {
    return new PearsPositionState(administratorIds, licenseeIds, Set.of(), Set.of());
  }

  private static LicencePositionState organisations(Integer administratorId, Set<Integer> licenseeIds) {
    return new LicencePositionState(administratorId, null, licenseeIds, Map.of(), Map.of(), Map.of());
  }

  private static LivePositionComparison.MatchedPosition matchedPosition(
      LocalDate positionDate,
      int positionSequence,
      int positionDateOrder,
      String regulatorReference
  ) {
    return new LivePositionComparison.MatchedPosition(
        new PearsLicencePositions.Position(positionDate, positionSequence, positionDateOrder, regulatorReference, 0),
        LicencePositionTestUtil.newBuilder()
            .withPositionDate(positionDate)
            .withPositionOrder(positionDateOrder)
            .withLicenceTransaction(LicenceTransactionTestUtil.newBuilder()
                .withRegulatorReference(regulatorReference)
                .build())
            .build()
    );
  }
}
