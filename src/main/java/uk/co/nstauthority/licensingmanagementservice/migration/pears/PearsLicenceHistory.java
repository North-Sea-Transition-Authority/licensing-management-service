package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.LicenceOperationHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

/**
 * Everything PEARS holds for one licence, as the positions the licence holds and the order it
 * holds them in: by position date, then by order within that date. Every transaction is a
 * position, including those carrying nothing this migration can use.
 */
public record PearsLicenceHistory(String licenceType, int licenceNo, List<PearsPosition> positions) {

  /**
   * The operations of one licence grouped into the positions it holds, in the order it holds them.
   */
  public static PearsLicenceHistory reconstruct(LicenceOperationHistory history) {
    return new PearsLicenceHistory(
        history.licenceType(),
        history.licenceNo(),
        positionsOf(history.operations())
    );
  }

  public String licenceReference() {
    return "%s%d".formatted(licenceType, licenceNo);
  }

  /**
   * These positions as the position comparison takes them, so a licence timeline built from a
   * history is checked against PEARS by the same code as one built from its data points.
   */
  public PearsLicencePositions toLicencePositions() {
    return new PearsLicencePositions(
        licenceType,
        licenceNo,
        positions.stream()
            .map(position -> new PearsLicencePositions.Position(
                position.positionDate(),
                position.positionSequence(),
                position.positionDateOrder(),
                position.regulatorReference(),
                position.transactionId()
            ))
            .toList());
  }

  private static List<PearsPosition> positionsOf(List<PearsOperation> operations) {
    var operationsByTransaction = new LinkedHashMap<Long, List<PearsOperation>>();
    for (var operation : operations) {
      operationsByTransaction
          .computeIfAbsent(operation.header().tranId(), tranId -> new ArrayList<PearsOperation>())
          .add(operation);
    }

    var unranked = operationsByTransaction.values().stream()
        .map(PearsLicenceHistory::unrankedPosition)
        .sorted(Comparator
            .comparing(PearsPosition::positionDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingInt(PearsPosition::positionSequence)
            .thenComparingLong(PearsPosition::transactionId))
        .toList();

    return reranked(unranked);
  }

  /**
   * One transaction's operations as a position, in the order PEARS made them. The position's own
   * details are the same on every operation of a transaction, so the first speaks for all of them.
   */
  private static PearsPosition unrankedPosition(List<PearsOperation> operations) {
    var ordered = operations.stream()
        .sorted(Comparator
            .comparingInt((PearsOperation operation) -> operation.header().operationSequence())
            .thenComparingLong(operation -> operation.header().operationId()))
        .toList();

    var header = ordered.getFirst().header();
    return new PearsPosition(
        header.tranId(),
        header.regulatorReference(),
        header.positionDate(),
        header.positionSequence(),
        0,
        ordered
    );
  }

  private static List<PearsPosition> reranked(List<PearsPosition> positions) {
    var ranks = PositionDateOrder.ranks(positions.stream().map(PearsPosition::positionDate).toList());

    var ranked = new ArrayList<PearsPosition>(positions.size());
    for (var index = 0; index < positions.size(); index++) {
      var position = positions.get(index);
      ranked.add(new PearsPosition(
          position.transactionId(),
          position.regulatorReference(),
          position.positionDate(),
          position.positionSequence(),
          ranks[index],
          position.operations()
      ));
    }
    return ranked;
  }
}
