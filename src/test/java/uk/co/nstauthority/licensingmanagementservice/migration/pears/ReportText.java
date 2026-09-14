package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.ArrayList;
import java.util.List;

/**
 * Shortens the lists that go into difference messages and summaries.
 *
 * <p>A licence can be short of eighty positions for the one reason, so a message naming all eighty
 * costs a screen and says nothing the count and a handful of examples do not.
 */
final class ReportText {

  /**
   * How many items of a list are worth naming before the rest become a count.
   */
  static final int SAMPLE_SIZE = 12;

  private ReportText() {
  }

  /**
   * The list as a comma separated string, with everything past {@link #SAMPLE_SIZE} collapsed into
   * a count.
   */
  static String sample(List<String> items) {
    if (items.size() <= SAMPLE_SIZE) {
      return String.join(", ", items);
    }
    return "%s, ... (+%,d more)".formatted(
        String.join(", ", items.subList(0, SAMPLE_SIZE)),
        items.size() - SAMPLE_SIZE
    );
  }

  /**
   * The list truncated to {@link #SAMPLE_SIZE} items plus a line saying how many were dropped, for
   * where the items are lines in their own right rather than a phrase.
   */
  static List<String> capped(List<String> items) {
    if (items.size() <= SAMPLE_SIZE) {
      return List.copyOf(items);
    }
    var capped = new ArrayList<>(items.subList(0, SAMPLE_SIZE));
    capped.add("... (+%,d more)".formatted(items.size() - SAMPLE_SIZE));
    return List.copyOf(capped);
  }
}
