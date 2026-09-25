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
      for (var entry : entriesOf(operation)) {
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

    // An organisation id is an Energy Portal organisation unit id, which this application holds as
    // an int. Nothing in PEARS should exceed that, and inventing an administrator out of a wrapped
    // id would be worse than not carrying one.
    if (joining.id() > Integer.MAX_VALUE || joining.id() < Integer.MIN_VALUE) {
      notes.unmapped("licence administrator organisation id too large to hold", operationId);
      return null;
    }

    return new AdministratorSet(
        new PearsOperationKey(operation.header().operationSequence(), operationId),
        Math.toIntExact(joining.id()));
  }

  private static boolean isAdministratorList(PearsOperation operation) {
    var companyListType = switch (operation) {
      case PearsOperation.ConsortiumListCreate create -> create.companyListType();
      case PearsOperation.ConsortiumListChange change -> change.companyListType();
      default -> null;
    };
    return PearsCompanyListType.LICENCE_ADMINISTRATOR.equals(companyListType);
  }

  private static List<PearsOperation.ConsortiumEntry> entriesOf(PearsOperation operation) {
    return switch (operation) {
      case PearsOperation.ConsortiumListCreate create -> create.entries();
      case PearsOperation.ConsortiumListChange change -> change.entries();
      default -> List.of();
    };
  }

  /**
   * One PEARS entry that sets the administrator, and where in its transaction it came.
   */
  private record AdministratorSet(PearsOperationKey at, int operatorId) {
  }
}
