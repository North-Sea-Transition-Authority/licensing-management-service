package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * A licence's PEARS work programme cannot be migrated as it is. The licence is skipped and reported with the reason,
 * and the rest of the run carries on.
 */
public class PearsWorkProgrammeMappingException extends RuntimeException {

  private final transient PearsLicenceReference licence;

  private final String reason;

  PearsWorkProgrammeMappingException(PearsLicenceReference licence, String reason) {
    super("%s: %s".formatted(licence, reason));
    this.licence = licence;
    this.reason = reason;
  }

  public PearsLicenceReference licence() {
    return licence;
  }

  public String reason() {
    return reason;
  }
}
