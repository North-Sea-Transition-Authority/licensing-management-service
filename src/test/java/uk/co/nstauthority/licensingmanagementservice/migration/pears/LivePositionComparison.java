package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

/**
 * Compares the licence timeline this application builds with the live positions PEARS holds.
 *
 * <p>{@link PearsLicencePositions} works out what PEARS holds, including the order a licence displays its
 * positions in; this works out whether the positions built from it arrived at the same thing. The
 * order is the interesting part: {@code LicenceWritebackService} replays PEARS' dates and
 * references through {@code LicencePositionService}, which derives {@code positionDateOrder}
 * itself rather than copying PEARS' sparse {@code positionSequence} across, so agreement is a
 * result rather than a given.
 *
 * <p>Positions are matched on regulator reference, which is a position's name in PEARS but not
 * quite an identifier: one transaction master can execute twice against a licence, on two dates,
 * under the one reference. Which occurrence stands for which is {@link PositionMatching}'s job.
 */
class LivePositionComparison {

  private static final String NO_REFERENCE = "(no reference)";

  private LivePositionComparison() {
  }

  /**
   * Every way the built timeline differs from the one PEARS holds, coarsest first. An empty list is
   * agreement.
   *
   * @param pearsLicencePositions licence positions from PEARS
   * @param licencePositions the licence's executed positions as this application built them
   */
  static List<PositionDifference> compare(PearsLicencePositions pearsLicencePositions, List<LicencePosition> licencePositions) {
    var lmsLicencePositions = licencePositions.stream()
        .sorted(Comparator.comparing(LicencePosition::getPositionDate).thenComparingInt(LicencePosition::getPositionDateOrder))
        .map(LmsLicencePosition::from)
        .toList();

    var matching = PositionMatching.of(pearsLicencePositions.positions(), lmsLicencePositions);

    var differences = new ArrayList<PositionDifference>();
    differences.addAll(unreferencedPositions(lmsLicencePositions));
    differences.addAll(unmatchedPositions(pearsLicencePositions.positions(), lmsLicencePositions, matching));
    differences.addAll(matchedPositionDifferences(pearsLicencePositions.positions(), lmsLicencePositions, matching));

    return List.copyOf(differences);
  }

  /**
   * Positions built without a reference, which cannot be matched against PEARS at all. Reported
   * separately so they are not read as positions PEARS does not hold.
   */
  private static List<PositionDifference> unreferencedPositions(List<LmsLicencePosition> lmsLicencePosition) {
    var unreferenced = lmsLicencePosition.stream()
        .filter(position -> NO_REFERENCE.equals(position.regulatorReference()))
        .toList();

    if (unreferenced.isEmpty()) {
      return List.of();
    }

    return List.of(new PositionDifference(
        PositionDifference.Kind.REGULATOR_REFERENCES,
        "%,d position(s) built without a regulator reference, so they cannot be matched to PEARS: %s".formatted(
            unreferenced.size(),
            ReportText.sample(unreferenced.stream().map(LmsLicencePosition::describe).toList())
        )
    ));
  }

  /**
   * The positions only one side holds, each collapsed into a single difference: a licence can be
   * short of eighty positions for the one reason.
   *
   * <p>These are the positions the matching could not account for, so the position named is the one
   * genuinely absent rather than whichever occurrence of its reference happened to fall last.
   */
  private static List<PositionDifference> unmatchedPositions(
      List<PearsLicencePositions.Position> pearsLicencePosition,
      List<LmsLicencePosition> lmsLicencePosition,
      PositionMatching matching
  ) {
    var missing = matching.unmatchedPearsIndexes().stream().map(pearsLicencePosition::get).toList();
    var extra = matching.unmatchedLmsIndexes().stream().map(lmsLicencePosition::get).toList();

    var differences = new ArrayList<PositionDifference>();

    if (!missing.isEmpty()) {
      differences.add(new PositionDifference(
          PositionDifference.Kind.POSITIONS,
          "%,d position(s) PEARS holds and this build does not: %s".formatted(
              missing.size(),
              ReportText.sample(missing.stream().map(LivePositionComparison::describe).toList())
          )
      ));
    }

    if (!extra.isEmpty()) {
      differences.add(new PositionDifference(
          PositionDifference.Kind.POSITIONS,
          "%,d position(s) built that PEARS does not hold: %s".formatted(
              extra.size(),
              ReportText.sample(extra.stream().map(LmsLicencePosition::describe).toList())
          )
      ));
    }

    return differences;
  }

  /**
   * The dates and orders of the positions both sides hold.
   *
   * <p>Walks PEARS' positions in the order it holds them so the differences come out
   * chronologically. A position sitting on the wrong date says nothing about its order within a
   * date the two do not agree on, so only one of the two is reported per position.
   */
  private static List<PositionDifference> matchedPositionDifferences(
      List<PearsLicencePositions.Position> pearsLicencePosition,
      List<LmsLicencePosition> lmsLicencePosition,
      PositionMatching matching
  ) {
    var differences = new ArrayList<PositionDifference>();
    var outOfOrder = new ArrayList<String>();

    for (var pearsIndex = 0; pearsIndex < pearsLicencePosition.size(); pearsIndex++) {
      var lmsIndex = matching.lmsIndexByPearsIndex().get(pearsIndex);
      if (lmsIndex == null) {
        continue;
      }

      var pearsPosition = pearsLicencePosition.get(pearsIndex);
      var lmsPosition = lmsLicencePosition.get(lmsIndex);
      var regulatorReference = pearsPosition.regulatorReference();

      if (!lmsPosition.positionDate().equals(pearsPosition.positionDate())) {
        differences.add(new PositionDifference(
            PositionDifference.Kind.POSITION_DATES,
            "%s sits on %s here and on %s in PEARS".formatted(
                regulatorReference, lmsPosition.positionDate(), pearsPosition.positionDate())
        ));
      } else if (lmsPosition.positionDateOrder() != pearsPosition.positionDateOrder()) {
        outOfOrder.add("%s is #%d on %s here and #%d in PEARS (sequence %d)".formatted(
            regulatorReference,
            lmsPosition.positionDateOrder(),
            pearsPosition.positionDate(),
            pearsPosition.positionDateOrder(),
            pearsPosition.positionSequence()
        ));
      }
    }

    // One position out of order shifts every position after it on the date, so only the first
    // divergence is reported -- the rest of the run follows from it.
    if (!outOfOrder.isEmpty()) {
      differences.add(new PositionDifference(
          PositionDifference.Kind.POSITION_ORDER,
          outOfOrder.size() == 1
              ? outOfOrder.getFirst()
              : "%s, and %,d more out of order".formatted(outOfOrder.getFirst(), outOfOrder.size() - 1)
      ));
    }

    return differences;
  }

  private static String describe(PearsLicencePositions.Position position) {
    return "%s (%s #%d)".formatted(position.regulatorReference(), position.positionDate(), position.positionDateOrder());
  }

  /**
   * Which built position stands for which of PEARS', and what neither side could account for, held
   * as indexes into the two lists so identical positions stay distinguishable.
   *
   * <p>A reference held once on each side is the whole of it. Where a reference repeats, the
   * occurrences the two sides agree the date of claim each other first, so a reference PEARS holds
   * on two dates and the build holds on one of them leaves the other date unmatched -- the
   * position genuinely missing -- rather than pairing the two in the order they are held and
   * reporting the date both sides agree on as wrong. Only the occurrences left after that are
   * paired in the order the sides hold them, nth to nth, which is what a position built on the
   * wrong date looks like.
   *
   * @param lmsIndexByPearsIndex the built position standing for each of PEARS' positions
   * @param unmatchedPearsIndexes PEARS' positions with no built position to stand for them
   * @param unmatchedLmsIndexes built positions standing for none of PEARS'
   */
  private record PositionMatching(
      Map<Integer, Integer> lmsIndexByPearsIndex,
      List<Integer> unmatchedPearsIndexes,
      List<Integer> unmatchedLmsIndexes
  ) {

    static PositionMatching of(List<PearsLicencePositions.Position> pearsLicencePosition, List<LmsLicencePosition> lmsLicencePosition) {
      var pearsIndexesByReference = indexesByReference(pearsLicencePosition.size(), index -> pearsLicencePosition.get(index).regulatorReference());
      var lmsIndexesByReference = indexesByReference(lmsLicencePosition.size(), index -> lmsLicencePosition.get(index).regulatorReference());

      // Positions built without a reference are reported as unmatchable in their own right, so
      // they are kept out of the matching rather than counted as positions PEARS does not hold.
      lmsIndexesByReference.remove(NO_REFERENCE);

      var lmsIndexByPearsIndex = new HashMap<Integer, Integer>();
      var unmatchedPearsIndexes = new ArrayList<Integer>();
      var unmatchedLmsIndexes = new ArrayList<Integer>();

      pearsIndexesByReference.forEach((regulatorReference, pearsIndexes) -> {
        var unclaimedPearsIndexes = new ArrayList<>(pearsIndexes);
        var unclaimedLmsIndexes = new ArrayList<>(lmsIndexesByReference.getOrDefault(regulatorReference, List.of()));

        claimOnPositionDate(pearsLicencePosition, lmsLicencePosition, unclaimedPearsIndexes, unclaimedLmsIndexes, lmsIndexByPearsIndex);
        claimInOrderHeld(unclaimedPearsIndexes, unclaimedLmsIndexes, lmsIndexByPearsIndex);

        unmatchedPearsIndexes.addAll(unclaimedPearsIndexes);
        unmatchedLmsIndexes.addAll(unclaimedLmsIndexes);
      });

      lmsIndexesByReference.forEach((regulatorReference, lmsIndexes) -> {
        if (!pearsIndexesByReference.containsKey(regulatorReference)) {
          unmatchedLmsIndexes.addAll(lmsIndexes);
        }
      });

      Collections.sort(unmatchedPearsIndexes);
      Collections.sort(unmatchedLmsIndexes);

      return new PositionMatching(lmsIndexByPearsIndex, unmatchedPearsIndexes, unmatchedLmsIndexes);
    }

    /**
     * Pairs the occurrences of one reference that sit on the same date, taking each pairing out of
     * what is left to claim.
     */
    private static void claimOnPositionDate(
        List<PearsLicencePositions.Position> pearsLicencePosition,
        List<LmsLicencePosition> lmsLicencePosition,
        List<Integer> unclaimedPearsIndexes,
        List<Integer> unclaimedLmsIndexes,
        Map<Integer, Integer> lmsIndexByPearsIndex
    ) {
      var pearsIterator = unclaimedPearsIndexes.iterator();
      while (pearsIterator.hasNext()) {
        var pearsIndex = pearsIterator.next();
        var positionDate = pearsLicencePosition.get(pearsIndex).positionDate();

        var claimed = -1;
        for (var candidate = 0; candidate < unclaimedLmsIndexes.size(); candidate++) {
          if (lmsLicencePosition.get(unclaimedLmsIndexes.get(candidate)).positionDate().equals(positionDate)) {
            claimed = candidate;
            break;
          }
        }

        if (claimed >= 0) {
          lmsIndexByPearsIndex.put(pearsIndex, unclaimedLmsIndexes.remove(claimed));
          pearsIterator.remove();
        }
      }
    }

    /**
     * Pairs whatever occurrences of one reference are left nth to nth, which is as much as can be
     * said about occurrences no date agrees on.
     */
    private static void claimInOrderHeld(
        List<Integer> unclaimedPearsIndexes,
        List<Integer> unclaimedLmsIndexes,
        Map<Integer, Integer> lmsIndexByPearsIndex
    ) {
      var paired = Math.min(unclaimedPearsIndexes.size(), unclaimedLmsIndexes.size());
      for (var occurrence = 0; occurrence < paired; occurrence++) {
        lmsIndexByPearsIndex.put(unclaimedPearsIndexes.get(occurrence), unclaimedLmsIndexes.get(occurrence));
      }

      unclaimedPearsIndexes.subList(0, paired).clear();
      unclaimedLmsIndexes.subList(0, paired).clear();
    }

    private static LinkedHashMap<String, List<Integer>> indexesByReference(int size, IntFunction<String> referenceOf) {
      var indexesByReference = new LinkedHashMap<String, List<Integer>>();
      for (var index = 0; index < size; index++) {
        indexesByReference.computeIfAbsent(referenceOf.apply(index), reference -> new ArrayList<Integer>()).add(index);
      }
      return indexesByReference;
    }
  }

  /**
   * One position as this application built it.
   */
  private record LmsLicencePosition(String regulatorReference, LocalDate positionDate, int positionDateOrder) {

    static LmsLicencePosition from(LicencePosition position) {
      String regulatorReference;
      var transaction = position.getLicenceTransaction();
      if (transaction == null || transaction.getRegulatorReference() == null) {
        regulatorReference = NO_REFERENCE;
      } else {
        regulatorReference = transaction.getRegulatorReference();
      }

      return new LmsLicencePosition(
          regulatorReference,
          position.getPositionDate(),
          position.getPositionDateOrder()
      );
    }

    private String describe() {
      return "%s (%s #%d)".formatted(regulatorReference, positionDate, positionDateOrder);
    }
  }
}
