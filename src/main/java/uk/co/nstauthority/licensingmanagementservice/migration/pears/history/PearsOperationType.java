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

  public static final String PED_BLOCK_CREATE = "PED_BLOCK_CREATE";

  public static final String PED_BLOCK_CHANGE = "PED_BLOCK_CHANGE";

  public static final String PED_BLOCK_END = "PED_BLOCK_END";

  private PearsOperationType() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }
}
