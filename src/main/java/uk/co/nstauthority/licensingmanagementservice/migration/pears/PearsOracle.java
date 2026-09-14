package uk.co.nstauthority.licensingmanagementservice.migration.pears;

/**
 * Which reading of PEARS a licence's timeline is being compared against.
 *
 * <p>The two answer different questions, so a sweep asks both. The operations are what the
 * migration itself reads, so comparing against them checks the replay rather than the reading:
 * whether anything was lost, merged or ordered differently on the way into this application's
 * tables. The data points are what PEARS holds in its own right, so comparing against them is the
 * only one of the two that can catch the reading itself being wrong.
 */
public enum PearsOracle {

  /**
   * The positions re-derived from the operations that made them, which is what the migration reads.
   */
  OPERATIONS("the operation-derived positions (checks the replay)"),

  /**
   * The positions PEARS holds in its own right, which is what the migration is validated against.
   */
  DATA_POINTS("the PEARS data points (checks the reading)");

  private final String label;

  PearsOracle(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
