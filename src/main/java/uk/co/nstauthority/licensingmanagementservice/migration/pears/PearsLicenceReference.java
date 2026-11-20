package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.regex.Pattern;

/**
 * One licence PEARS holds live positions for.
 *
 * @param licenceType the licence's prefix, for example {@code P}
 * @param licenceNo   the licence's number, for example {@code 1}
 */
public record PearsLicenceReference(String licenceType, int licenceNo) {

  private static final Pattern LICENCE_REFERENCE = Pattern.compile("([A-Z]+)(\\d+)");

  /**
   * The reference of a licence written as LMS writes it, for example {@code P2244} or {@code PEDL236}.
   *
   * @throws IllegalArgumentException if it isn't a licence type followed by a number
   */
  public static PearsLicenceReference parse(String licenceReference) {
    var matcher = LICENCE_REFERENCE.matcher(licenceReference);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("%s is not a PEARS licence reference".formatted(licenceReference));
    }
    return new PearsLicenceReference(matcher.group(1), Integer.parseInt(matcher.group(2)));
  }

  public String licenceReference() {
    return "%s%d".formatted(licenceType, licenceNo);
  }

  @Override
  public String toString() {
    return licenceReference();
  }
}
