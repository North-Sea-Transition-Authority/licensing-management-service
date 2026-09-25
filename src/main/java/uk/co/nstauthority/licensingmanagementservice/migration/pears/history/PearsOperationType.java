package uk.co.nstauthority.licensingmanagementservice.migration.pears.history;

/**
 * The PEARS OPERATION_TYPE values this migration names. Constants rather than an enum, because
 * PEARS is free to add types this migration does not know about.
 */
public final class PearsOperationType {

  public static final String LICENCE_CREATE = "LICENCE_CREATE";

  public static final String LICENCE_END = "LICENCE_END";

  public static final String CONSORTIUM_LIST_CREATE = "CONSORTIUM_LIST_CREATE";

  public static final String CONSORTIUM_LIST_CHANGE = "CONSORTIUM_LIST_CHANGE";

  private PearsOperationType() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }
}
