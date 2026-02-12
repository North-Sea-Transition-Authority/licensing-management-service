package uk.co.nstauthority.licensingmanagementservice.licence;

import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Comparator for licence references (e.g. "P5", "CS10") that sorts by
 * alphabetic prefix first, then by numeric value.
 */
public class LicenceReferenceComparator implements Comparator<String> {

  private static final Pattern LICENCE_REF_PATTERN = Pattern.compile("^([A-Za-z]+)(\\d+)$");

  @Override
  public int compare(String left, String right) {
    var leftMatcher = LICENCE_REF_PATTERN.matcher(left);
    var rightMatcher = LICENCE_REF_PATTERN.matcher(right);

    if (!leftMatcher.matches() || !rightMatcher.matches()) {
      return left.compareToIgnoreCase(right);
    }

    int prefixComparison = leftMatcher.group(1).compareToIgnoreCase(rightMatcher.group(1));
    if (prefixComparison != 0) {
      return prefixComparison;
    }

    return Integer.compare(
        Integer.parseInt(leftMatcher.group(2)),
        Integer.parseInt(rightMatcher.group(2))
    );
  }
}
