package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

/**
 * Compares what the licence holds at each position this application builds with what PEARS holds at the same position:
 * its administrator, its licensees, its blocks and the subareas dividing them. Only positions both sides hold are
 * compared, so nothing here repeats what {@link LivePositionComparison} says about positions only one side holds.
 *
 * <p>Blocks and subareas are compared by PEARS SI_ID, which a migrated GIS feature carries as its legacy id. A block
 * PEARS holds with no migrated feature cannot be held by any position here, so it is counted rather than compared.
 */
class PositionStateComparison {

  private PositionStateComparison() {
  }

  /**
   * @param positionsCompared      positions whose holdings were compared
   * @param positionsHoldingNothing positions where PEARS holds nothing at all, which is how it records a licence
   *                                ending. Not compared until ending a licence is migrated.
   * @param blocksWithoutFeature   the blocks PEARS holds that have no migrated GIS feature, and so were not compared
   * @param differences            one per kind of holding that differs, at the first position it differs at
   */
  record Result(
      int positionsCompared,
      int positionsHoldingNothing,
      int blocksWithoutFeature,
      List<PositionDifference> differences
  ) {
  }

  /**
   * @param matched              the positions both sides hold
   * @param pearsStates          what PEARS holds at each of its positions
   * @param lmsStates            what this application holds at each of its positions
   * @param legacyIdByFeatureId  the legacy id of every GIS feature either side holds, resolved up front
   */
  static Result compare(
      List<LivePositionComparison.MatchedPosition> matched,
      Map<PearsLicencePositions.PositionKey, PearsPositionState> pearsStates,
      ResolvedStates lmsStates,
      Map<UUID, Integer> legacyIdByFeatureId
  ) {
    // A HashSet, since an immutable set throws when asked whether it holds the null SI_ID of a shapeless block.
    var migratedSiIds = new HashSet<>(legacyIdByFeatureId.values());
    var divergences = new EnumMap<PositionDifference.Kind, Divergence>(PositionDifference.Kind.class);
    var blocksWithoutFeature = new HashSet<Integer>();
    var positionsCompared = 0;
    var positionsHoldingNothing = 0;

    for (var matchedPosition : matched) {
      var pearsPosition = matchedPosition.pearsPosition();
      var pearsState = pearsStates.getOrDefault(pearsPosition.key(), PearsPositionState.EMPTY);
      if (pearsState.holdsNothing()) {
        positionsHoldingNothing++;
        continue;
      }
      positionsCompared++;

      var lmsState = lmsStates.currentState(matchedPosition.licencePosition().getId());
      var position = pearsPosition.describe();

      pearsState.blockSiIds().stream()
          .filter(siId -> !migratedSiIds.contains(siId))
          .forEach(blocksWithoutFeature::add);

      compareAdministrators(position, lmsState, pearsState, divergences);
      compareHeld(PositionDifference.Kind.LICENSEES, position,
          organisations(lmsState.licenseeIds()), organisations(pearsState.licenseeIds()), divergences);
      compareHeld(PositionDifference.Kind.BLOCKS, position,
          lmsBlocks(lmsState, legacyIdByFeatureId), pearsBlocks(pearsState, migratedSiIds), divergences);
      compareHeld(PositionDifference.Kind.SUBAREAS, position,
          lmsSubareas(lmsState, legacyIdByFeatureId), pearsSubareas(pearsState, migratedSiIds), divergences);
    }

    var differences = divergences.entrySet().stream()
        .map(entry -> new PositionDifference(entry.getKey(), entry.getValue().describe()))
        .toList();

    return new Result(positionsCompared, positionsHoldingNothing, blocksWithoutFeature.size(), differences);
  }

  private static void compareAdministrators(
      String position,
      LicencePositionState lmsState,
      PearsPositionState pearsState,
      Map<PositionDifference.Kind, Divergence> divergences
  ) {
    var lmsAdministrators = lmsState.administratorId() == null
        ? Set.<String>of()
        : Set.of(String.valueOf(lmsState.administratorId()));
    var pearsAdministrators = organisations(pearsState.administratorIds());

    if (!lmsAdministrators.equals(pearsAdministrators)) {
      diverge(divergences, PositionDifference.Kind.ADMINISTRATORS, "%s is administered by %s here and by %s in PEARS"
          .formatted(position, listed(lmsAdministrators), listed(pearsAdministrators)));
    }
  }

  /**
   * Compares two sets of holdings, saying what only one side holds.
   */
  private static void compareHeld(
      PositionDifference.Kind kind,
      String position,
      Set<String> lmsHeld,
      Set<String> pearsHeld,
      Map<PositionDifference.Kind, Divergence> divergences
  ) {
    if (lmsHeld.equals(pearsHeld)) {
      return;
    }

    var onlyPears = pearsHeld.stream().filter(held -> !lmsHeld.contains(held)).toList();
    var onlyLms = lmsHeld.stream().filter(held -> !pearsHeld.contains(held)).toList();

    var sides = new ArrayList<String>();
    if (!onlyPears.isEmpty()) {
      sides.add("only PEARS holds %s".formatted(listed(onlyPears)));
    }
    if (!onlyLms.isEmpty()) {
      sides.add("only this application holds %s".formatted(listed(onlyLms)));
    }

    diverge(divergences, kind, "%s: %s".formatted(position, String.join("; ", sides)));
  }

  /**
   * Records that a kind of holding differs at a position. A licensee held wrongly at one position is usually held
   * wrongly at every position after it, so only the first is described and the rest are counted.
   */
  private static void diverge(Map<PositionDifference.Kind, Divergence> divergences, PositionDifference.Kind kind, String detail) {
    var divergence = divergences.get(kind);
    if (divergence == null) {
      divergences.put(kind, new Divergence(detail));
    } else {
      divergence.laterPositions++;
    }
  }

  private static Set<String> organisations(Collection<Integer> organisationUnitIds) {
    return organisationUnitIds.stream().map(String::valueOf).collect(Collectors.toSet());
  }

  private static Set<String> pearsBlocks(PearsPositionState pearsState, Set<Integer> migratedSiIds) {
    return pearsState.blockSiIds().stream()
        .filter(migratedSiIds::contains)
        .map(PositionStateComparison::siId)
        .collect(Collectors.toSet());
  }

  private static Set<String> lmsBlocks(LicencePositionState lmsState, Map<UUID, Integer> legacyIdByFeatureId) {
    return lmsState.blockFeatureIds().stream()
        .map(featureId -> feature(featureId, legacyIdByFeatureId))
        .collect(Collectors.toSet());
  }

  /**
   * The subareas PEARS holds on blocks that have a migrated feature. Represented as strings for ease of comparison
   */
  private static Set<String> pearsSubareas(PearsPositionState pearsState, Set<Integer> migratedSiIds) {
    return pearsState.subareas().stream()
        .filter(subarea -> migratedSiIds.contains(subarea.blockSiId()))
        .map(subarea -> subarea(
            siId(subarea.blockSiId()),
            subarea.shortName(),
            migratedSiIds.contains(subarea.siId()) ? siId(subarea.siId()) : null
        ))
        .collect(Collectors.toSet());
  }

  /**
   * The subareas on LMS represented as strings for ease of comparison
   */
  private static Set<String> lmsSubareas(LicencePositionState lmsState, Map<UUID, Integer> legacyIdByFeatureId) {
    var subareas = new HashSet<String>();
    lmsState.blockFeatureIdToSubareas().forEach((blockFeatureId, blockSubareas) -> {
      var block = feature(blockFeatureId, legacyIdByFeatureId);
      for (SubareaDetails subarea : blockSubareas) {
        var reference = subarea.featureId() == null ? null : feature(subarea.featureId(), legacyIdByFeatureId);
        subareas.add(subarea(block, subarea.shortName(), reference));
      }
    });
    return subareas;
  }

  private static String feature(UUID featureId, Map<UUID, Integer> legacyIdByFeatureId) {
    var legacyId = legacyIdByFeatureId.get(featureId);
    return legacyId == null ? "feature %s".formatted(featureId) : siId(legacyId);
  }

  private static String siId(Integer siId) {
    return "SI %d".formatted(siId);
  }

  private static String subarea(String block, String shortName, String reference) {
    return reference == null
        ? "%s/%s".formatted(block, shortName)
        : "%s/%s (%s)".formatted(block, shortName, reference);
  }

  private static String listed(Collection<String> held) {
    if (held.isEmpty()) {
      return "nobody";
    }
    return ReportText.sample(held.stream().sorted().toList());
  }

  /**
   * The first position a kind of holding differs at, and how many positions after it differ too.
   */
  private static final class Divergence {

    private final String first;
    private int laterPositions;

    private Divergence(String first) {
      this.first = first;
    }

    private String describe() {
      return laterPositions == 0
          ? first
          : "%s, and at %,d later position(s)".formatted(first, laterPositions);
    }
  }
}
