package uk.co.nstauthority.licensingmanagementservice.migration.pears;

/**
 * One way in which the licence timeline this application builds differs from the one PEARS holds.
 * The kind is an enum rather than a prefix on the detail so a summary can group by it.
 */
public record PositionDifference(Kind kind, String detail) {

  public enum Kind {

    POSITIONS,
    POSITION_DATES,
    POSITION_ORDER,
    ADMINISTRATORS,
    LICENSEES,
    BLOCKS,
    SUBAREAS,
    REGULATOR_REFERENCES;

    public String label() {
      return name().replace("_", " ").toLowerCase();
    }
  }

  @Override
  public String toString() {
    return "%s: %s".formatted(kind.label(), detail);
  }
}
