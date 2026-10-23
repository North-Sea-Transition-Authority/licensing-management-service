package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsPosition;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsCompanyListType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperationType;

/**
 * Carries a licence's administrator across from PEARS, where it is not an operation type of its
 * own but a company list whose COMPANY_LIST_TYPE is LICENCE_ADMINISTRATOR.
 *
 * <p>Administrator state is single valued, which shapes every judgement here: a position holds at
 * most one administrator change, removing an administrator is not an operation, and setting the
 * one the licence already has is not carried across.
 */
@Component
@Order(100)
class AdministratorOperationMigrator implements PearsOperationMigrator {

  @Override
  public String name() {
    return "licence administrator";
  }

  @Override
  public Set<String> pearsOperationTypes() {
    return Set.of(PearsOperationType.CONSORTIUM_LIST_CREATE, PearsOperationType.CONSORTIUM_LIST_CHANGE);
  }

  @Override
  public boolean supports(PearsOperation operation) {
    return isAdministratorList(operation);
  }

  @Override
  public List<MigratedChange> migrate(PearsLicenceHistory history, MigrationNotes notes) {
    var changes = new ArrayList<MigratedChange>();

    // The administrator the licence holds as the history is walked. A change that does not move it
    // is not a change, and this application rejects one that claims to.
    Integer administratorId = null;

    for (PearsPosition position : history.positions()) {
      var administratorSet = lastAdministratorSet(position, notes);
      if (administratorSet == null) {
        continue;
      }

      if (Objects.equals(administratorId, administratorSet.operatorId())) {
        notes.note("licence administrator set to the administrator already held, which is not a change");
        continue;
      }

      administratorId = administratorSet.operatorId();
      changes.add(MigratedChange.from(
          new PearsPositionKey(position.transactionId()),
          administratorSet.at(),
          List.of(LicenceOperation.newAdministratorChange().withOperator(administratorId).build())
      ));
    }

    return changes;
  }

  /**
   * The administrator a transaction left behind, or null where it did not touch it. A transaction
   * can hold several administrator operations; a position holds one change, so the last stands.
   */
  private static AdministratorSet lastAdministratorSet(PearsPosition position, MigrationNotes notes) {
    AdministratorSet last = null;
    var operationsSeen = 0;

    for (var operation : position.operations()) {
      if (!isAdministratorList(operation)) {
        continue;
      }
      operationsSeen++;
      for (var entry : ConsortiumLists.entriesOf(operation)) {
        var administratorSet = administratorSet(operation, entry, notes);
        if (administratorSet != null) {
          last = administratorSet;
        }
      }
    }

    if (operationsSeen > 1) {
      notes.note("several licence administrator operations in one transaction reduced to the last");
    }
    return last;
  }

  /**
   * One consortium entry as an administrator change, or null where it is not one.
   */
  private static AdministratorSet administratorSet(
      PearsOperation operation,
      PearsOperation.ConsortiumEntry entry,
      MigrationNotes notes
  ) {
    var operationId = operation.header().operationId();

    if (entry.type() == null) {
      notes.unmapped("licence administrator entry with no ENTRY_TYPE", operationId);
      return null;
    }

    var joining = switch (entry.type()) {
      case SET -> entry.primary();
      // The joining administrator is the secondary side of a transfer; the withdrawing one is
      // implied by the state it replaces.
      case TRANSFER -> entry.secondary();
      case REMOVE -> null;
    };

    if (entry.type() == PearsOperation.EntryType.REMOVE) {
      notes.note("licence administrator REMOVE dropped, because administrator state is single valued");
      return null;
    }

    if (joining == null) {
      notes.unmapped("licence administrator entry with no joining organisation", operationId);
      return null;
    }

    var operatorId = ConsortiumLists.organisationUnitId(joining);
    return new AdministratorSet(PearsOperationKey.of(operation), operatorId);
  }

  private static boolean isAdministratorList(PearsOperation operation) {
    return ConsortiumLists.isList(operation, PearsCompanyListType.LICENCE_ADMINISTRATOR);
  }

  /**
   * One PEARS entry that sets the administrator, and where in its transaction it came.
   */
  private record AdministratorSet(PearsOperationKey at, int operatorId) {
  }
}
