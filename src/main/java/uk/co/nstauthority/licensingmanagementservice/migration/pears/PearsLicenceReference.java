package uk.co.nstauthority.licensingmanagementservice.migration.pears;

/**
 * One licence PEARS holds live positions for.
 *
 * @param licenceType the licence's prefix, for example {@code P}
 * @param licenceNo   the licence's number, for example {@code 1}
 */
public record PearsLicenceReference(String licenceType, int licenceNo) {

  public String licenceReference() {
    return "%s%d".formatted(licenceType, licenceNo);
  }

  @Override
  public String toString() {
    return licenceReference();
  }
}
