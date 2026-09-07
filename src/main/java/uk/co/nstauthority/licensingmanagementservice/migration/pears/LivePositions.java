package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Everything PEARS holds for one licence, as the positions the licence holds and the order it holds
 * them in: by position date, then by order within that date.
 *
 * <p>In PEARS a position is a licence, a position date, and a sequence within that date, reached by
 * executing a transaction against the licence. {@code live-positions.sql} returns one row per
 * operation, so a position appears once for each operation that made it, and
 * {@link #reconstruct(List)} groups those rows back up into the positions themselves. Reading the
 * rows out of the PEARS database is {@code PearsLicenceService}'s job - this record is only the
 * shape they are read into, and the grouping over them.
 *
 * <p>{@code LicenceWritebackService} is what the result is for. It clears out the positions the
 * licence holds in this application and replays these in order, taking nothing from each but
 * its date and its transaction's regulator reference, and letting the application's own services
 * settle everything else. That is what leaves a migrated licence indistinguishable from one built
 * here.
 *
 * <p>The sequence PEARS gives a position orders transactions on a date across the whole live
 * simulation rather than within one licence, so the values a licence sees are sparse. The order
 * the licence displays is therefore the rank of the sequence within its date
 * ({@link Position#positionDateOrder()}), which is the number a replay should independently arrive
 * at for {@code LicencePosition.positionDateOrder}; {@link Position#positionSequence()} is kept
 * alongside it as the thing that decided the rank.
 *
 * <p>The regulator reference is the position's name in PEARS, and the natural thing to compare
 * positions by, though not quite an identifier: one transaction master can execute twice against a
 * licence, on two dates, under one reference.
 */
record LivePositions(
    String licenceType,
    int licenceNo,
    List<Position> positions
) {

  /**
   * One row of {@code live-positions.sql}: an operation, described by the position it belongs to.
   * Nothing but the position is read out of a row, so the several rows of a position made by
   * several operations are identical to each other.
   */
  public record Row(
      String licenceType,
      int licenceNo,
      String regulatorReference,
      LocalDate positionDate,
      int positionSequence
  ) {
  }

  /**
   * One live position of the licence.
   *
   * @param positionDate       the date PEARS holds the position on
   * @param positionSequence   the position's sequence within that date across the live simulation
   * @param positionDateOrder  the rank of {@code positionSequence} within the position date, from 1
   * @param regulatorReference the reference of the transaction that reached the position
   */
  public record Position(
      LocalDate positionDate,
      int positionSequence,
      int positionDateOrder,
      String regulatorReference
  ) {
  }

  /**
   * The rows of a single licence grouped into the positions it holds, in the order it holds them.
   *
   * @throws IllegalStateException if the rows hold no licence, or more than one
   */
  public static LivePositions reconstruct(List<Row> rows) {
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
  private static LivePositions forOneLicence(List<Row> rows) {
    var firstRows = new HashMap<PositionKey, Row>();
    for (var row : rows) {
      firstRows.putIfAbsent(new PositionKey(row.positionDate(), row.positionSequence()), row);
    }

    var keys = new ArrayList<>(firstRows.keySet());
    keys.sort(Comparator.comparing(PositionKey::positionDate).thenComparingInt(PositionKey::positionSequence));

    // The sequence numbers a licence sees are the live simulation's, so they are
    // ranked within the date to get the order the licence holds its positions in.
    var orderWithinDate = new HashMap<LocalDate, Integer>();
    var positions = new ArrayList<Position>(keys.size());
    for (var key : keys) {
      var order = orderWithinDate.merge(key.positionDate(), 1, Integer::sum);
      positions.add(new Position(
          key.positionDate(),
          key.positionSequence(),
          order,
          firstRows.get(key).regulatorReference()
      ));
    }

    var first = rows.getFirst();
    return new LivePositions(first.licenceType(), first.licenceNo(), positions);
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
