package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * The positions PEARS holds for one licence, as the licence holds them: by position date, then by
 * order within that date. Both readings of PEARS -- its data points and its operation history --
 * are compared in this shape.
 */
public record PearsLicencePositions(
    String licenceType,
    int licenceNo,
    List<Position> positions
) {

  /**
   * One row of {@code data-point-positions.sql}: a position, as PEARS' own record of the licence
   * holding one.
   */
  public record Row(
      String licenceType,
      int licenceNo,
      String regulatorReference,
      LocalDate positionDate,
      int positionSequence,
      long transactionId
  ) {
  }

  /**
   * One position of the licence.
   *
   * @param positionDate       the date PEARS holds the position on
   * @param positionSequence   the position's sequence within that date across the live simulation
   * @param positionDateOrder  the rank of {@code positionSequence} within the position date, from 1
   * @param regulatorReference the reference of the transaction that reached the position
   * @param transactionId      the transaction that reached the position, which positions are matched by
   */
  public record Position(
      LocalDate positionDate,
      int positionSequence,
      int positionDateOrder,
      String regulatorReference,
      long transactionId
  ) {
  }

  /**
   * The rows of a single licence grouped into the positions it holds, in the order it holds them.
   *
   * @throws IllegalStateException if the rows hold no licence, or more than one
   */
  public static PearsLicencePositions reconstruct(List<Row> rows) {
    var licences = rows.stream().map(row -> new LicenceKey(row.licenceType(), row.licenceNo())).distinct().toList();
    if (licences.size() != 1) {
      throw new IllegalStateException("Expected rows for exactly one licence but found " + licences);
    }
    return forOneLicence(rows);
  }

  public String licenceReference() {
    return "%s%d".formatted(licenceType, licenceNo);
  }

  /**
   * One licence's positions, in the order it holds them: by date, then by sequence within the date.
   */
  private static PearsLicencePositions forOneLicence(List<Row> rows) {
    var firstRows = new HashMap<PositionKey, Row>();
    for (var row : rows) {
      firstRows.putIfAbsent(new PositionKey(row.positionDate(), row.positionSequence()), row);
    }

    var keys = new ArrayList<>(firstRows.keySet());
    keys.sort(Comparator.comparing(PositionKey::positionDate).thenComparingInt(PositionKey::positionSequence));

    var ranks = PositionDateOrder.ranks(keys.stream().map(PositionKey::positionDate).toList());

    var positions = new ArrayList<Position>();
    for (var index = 0; index < keys.size(); index++) {
      var key = keys.get(index);
      var row = firstRows.get(key);
      positions.add(new Position(
          key.positionDate(),
          key.positionSequence(),
          ranks[index],
          row.regulatorReference(),
          row.transactionId()));
    }

    var first = rows.getFirst();
    return new PearsLicencePositions(first.licenceType(), first.licenceNo(), positions);
  }

  /**
   * What makes a position one position, within a licence.
   */
  private record PositionKey(LocalDate positionDate, int positionSequence) {
  }

  /**
   * Which licence a row belongs to, so rows that span more than one can be rejected.
   */
  private record LicenceKey(String licenceType, int licenceNo) {
  }
}
