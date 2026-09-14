package uk.co.nstauthority.licensingmanagementservice.migration.pears;

/**
 * One way in which the licence timeline this application builds differs from the one PEARS holds.
 *
 * <p>The kind is what a summary groups by, so it is an enum rather than a prefix on the detail: a
 * report bucketing differences should not have to parse them back out of a sentence.
 */
public record PositionDifference(Kind kind, String detail) {

  /**
   * What differs. Ordered from the coarsest difference to the finest, since a licence holding the
   * wrong positions makes any statement about their dates or order beside the point.
   */
  public enum Kind {

    /**
     * PEARS holds positions this application does not build, or the other way round.
     */
    POSITIONS("positions"),

    /**
     * A position both hold sits on a different date.
     */
    POSITION_DATES("position dates"),

    /**
     * A position both hold, on the date both agree on, is ordered differently within that date.
     */
    POSITION_ORDER("position order"),

    /**
     * A position was built that carries no regulator reference, so it cannot be matched at all.
     */
    REGULATOR_REFERENCES("regulator references");

    private final String label;

    Kind(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  @Override
  public String toString() {
    return "%s: %s".formatted(kind.label(), detail);
  }
}
