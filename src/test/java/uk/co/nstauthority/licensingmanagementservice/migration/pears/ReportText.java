package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shortens the lists that go into difference messages and summaries. A licence can be short of eighty positions for
 * the one reason, and naming all eighty says nothing the count and a handful of examples do not.
 */
final class ReportText {

  /**
   * How many items of a list are worth naming before the rest become a count.
   */
  static final int SAMPLE_SIZE = 12;

  private static final Pattern LICENCE_REFERENCE = Pattern.compile("([A-Z]+)(\\d+)");

  /**
   * Licence references in the order the sweep reads them from PEARS, by licence type and then number, so a summary
   * reads the same however the licences' comparisons happened to finish.
   */
  static final Comparator<String> SWEEP_ORDER = Comparator
      .comparing(ReportText::licenceType)
      .thenComparingLong(ReportText::licenceNumber)
      .thenComparing(Comparator.naturalOrder());

  private ReportText() {
  }

  private static String licenceType(String licenceReference) {
    var matcher = LICENCE_REFERENCE.matcher(licenceReference);
    return matcher.matches() ? matcher.group(1) : licenceReference;
  }

  private static long licenceNumber(String licenceReference) {
    var matcher = LICENCE_REFERENCE.matcher(licenceReference);
    return matcher.matches() ? Long.parseLong(matcher.group(2)) : 0;
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
      return items;
    }
    var capped = new ArrayList<>(items.subList(0, SAMPLE_SIZE));
    capped.add("... (+%,d more)".formatted(items.size() - SAMPLE_SIZE));
    return capped;
  }
}
