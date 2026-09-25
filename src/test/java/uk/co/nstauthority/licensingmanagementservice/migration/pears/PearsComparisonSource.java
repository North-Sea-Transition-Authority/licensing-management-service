package uk.co.nstauthority.licensingmanagementservice.migration.pears;

/**
 * Which reading of PEARS a licence's timeline is being compared against. Comparing against the
 * operations checks the replay; comparing against the data points checks the reading too.
 */
enum PearsComparisonSource {

  /**
   * The positions re-derived from the operations that made them, which is what the migration reads.
   */
  OPERATIONS("the operation-derived positions (checks the replay)"),

  /**
   * The positions PEARS holds in its own right, which is what the migration is validated against.
   */
  DATA_POINTS("the PEARS data points (checks the reading)");

  private final String label;

  PearsComparisonSource(String label) {
    this.label = label;
  }

  String label() {
    return label;
  }
}
