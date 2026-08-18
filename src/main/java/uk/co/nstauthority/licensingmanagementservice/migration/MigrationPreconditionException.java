package uk.co.nstauthority.licensingmanagementservice.migration;

/**
 * Thrown when a migration is triggered before the data it depends on is in place — for example running the carbon
 * storage schedule migration before its extract tables have been loaded, which would otherwise create an empty
 * schedule for every carbon storage licence and block a subsequent correct run.
 */
public class MigrationPreconditionException extends RuntimeException {

  public MigrationPreconditionException(String message) {
    super(message);
  }
}
