package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.time.LocalDate;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

/**
 * One position PEARS holds for a licence, with the operations the transaction that reached it
 * made. Every operation is here, whether or not this migration has anything to make of it.
 *
 * @param positionSequence  the position's sequence within its date across the live simulation,
 *                          which is sparse for any one licence
 * @param positionDateOrder the rank of {@code positionSequence} within the position date, from 1
 * @param operations        this transaction's operations, in the order PEARS made them
 */
public record PearsPosition(
    long transactionId,
    String regulatorReference,
    LocalDate positionDate,
    int positionSequence,
    int positionDateOrder,
    List<PearsOperation> operations
) {

  public String describe() {
    return "%s (%s #%d)".formatted(
        regulatorReference == null ? "(no reference)" : regulatorReference, positionDate, positionDateOrder);
  }
}
