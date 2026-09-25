package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.List;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;

/**
 * One kind of thing PEARS holds, carried across into the licence this application builds. Adding
 * an operation to the migration is adding one class that says which PEARS operations it needs and
 * what it makes of them.
 *
 * <p>Implementations are Spring beans, ordered by {@code @Order}, and that order is the tiebreak
 * between two migrators producing a change at the same place in PEARS' order.
 */
public interface PearsOperationMigrator {

  /**
   * What this migrator carries across, as a report names it. Lower case, since it reads inside a
   * sentence.
   */
  String name();

  /**
   * The PEARS OPERATION_TYPEs whose XML this migrator needs. Every operation still arrives whatever
   * its type, but one of a type nobody asked for arrives knowing only its type.
   */
  Set<String> pearsOperationTypes();

  /**
   * Whether this migrator carries the given operation across. An operation no migrator supports is
   * ignored: its position is still built, but nothing it did is.
   */
  boolean supports(PearsOperation operation);

  /**
   * The changes this licence's history calls for, each naming the position it lands on and the PEARS
   * operation whose place it takes. Anything that cannot be carried across goes to {@code notes}
   * rather than being silently dropped.
   */
  List<MigratedChange> migrate(PearsLicenceHistory history, MigrationNotes notes);
}
