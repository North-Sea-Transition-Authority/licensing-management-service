package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

/**
 * The order a licence holds its positions in within a date. PEARS' sequence orders the whole live
 * simulation rather than one licence, so the order shown is the rank of that sequence within its
 * date, which is what a replay through {@code LicencePositionService} arrives at.
 */
final class PositionDateOrder {

  private PositionDateOrder() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  /**
   * The rank within its date of each position, from 1, for positions already in the order the
   * licence holds them: by date, then by sequence within the date.
   *
   * @param positionDates the date of each position, in that order
   * @return the rank of each position, in the same order
   */
  static int[] ranks(List<LocalDate> positionDates) {
    var orderWithinDate = new HashMap<LocalDate, Integer>();
    var ranks = new int[positionDates.size()];
    for (var index = 0; index < positionDates.size(); index++) {
      ranks[index] = orderWithinDate.merge(positionDates.get(index), 1, Integer::sum);
    }
    return ranks;
  }
}
