package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * A licence's PEARS schedule cannot be migrated as it is. The licence is skipped and reported with the reason, and the
 * rest of the run carries on.
 */
public class PearsScheduleMappingException extends RuntimeException {

  /**
   * The reason for a schedule with no numbered term, which isn't migrated: the licensing team creates its LMS schedule
   * by hand.
   */
  public static final String NO_TERMS = "No terms in PEARS";

  private final transient PearsLicenceReference licence;

  private final String reason;

  PearsScheduleMappingException(PearsLicenceReference licence, String reason) {
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
