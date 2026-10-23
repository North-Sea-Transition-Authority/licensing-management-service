package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
 * Carries a licence's licensees joining and withdrawing across from PEARS, where they are not an
 * operation type of their own but a company list whose COMPANY_LIST_TYPE is LICENSEE.
 *
 * <p>Every entry is a step from the licensees the licence already holds, whether the list was
 * created or changed: SET joins an organisation, REMOVE withdraws one, and TRANSFER withdraws the
 * primary organisation for the secondary. A position holds one licensee change, so what a
 * transaction did is taken as the difference between the licensees held before it and after it.
 * That difference never joins a licensee already held, never withdraws one not held, and never
 * joins and withdraws the same organisation, none of which this application accepts.
 */
@Component
@Order(150)
class LicenseeOperationMigrator implements PearsOperationMigrator {

  @Override
  public String name() {
    return "licensee";
  }

  @Override
  public Set<String> pearsOperationTypes() {
    return Set.of(PearsOperationType.CONSORTIUM_LIST_CREATE, PearsOperationType.CONSORTIUM_LIST_CHANGE);
  }

  @Override
  public boolean supports(PearsOperation operation) {
    return isLicenseeList(operation);
  }

  @Override
  public List<MigratedChange> migrate(PearsLicenceHistory history, MigrationNotes notes) {
    var changes = new ArrayList<MigratedChange>();

    // The licensees the licence holds as the history is walked.
    var licenseeIds = new LinkedHashSet<Integer>();

    for (PearsPosition position : history.positions()) {
      var licenseeOperations = position.operations().stream()
          .filter(LicenseeOperationMigrator::isLicenseeList)
          .toList();

      if (licenseeOperations.isEmpty()) {
        continue;
      }

      if (licenseeOperations.size() > 1) {
        notes.note("several licensee operations in one transaction combined into one change");
      }

      var licenseeIdsBefore = new LinkedHashSet<>(licenseeIds);
      for (var operation : licenseeOperations) {
        for (var entry : ConsortiumLists.entriesOf(operation)) {
          apply(operation, entry, licenseeIds, notes);
        }
      }

      var licenseesToAdd = licenseeIds.stream().filter(id -> !licenseeIdsBefore.contains(id)).toList();
      var licenseesToRemove = licenseeIdsBefore.stream().filter(id -> !licenseeIds.contains(id)).toList();

      if (licenseesToAdd.isEmpty() && licenseesToRemove.isEmpty()) {
        notes.note("licensee operations that leave the licensees unchanged, which is not a change");
        continue;
      }

      if (licenseeIds.isEmpty()) {
        notes.note("licensee change leaves the licence with no licensees");
      }

      changes.add(MigratedChange.from(
          new PearsPositionKey(position.transactionId()),
          PearsOperationKey.of(licenseeOperations.getFirst()),
          List.of(LicenceOperation.newLicenseeOperation()
              .withLicenseesToAdd(licenseesToAdd)
              .withLicenseesToRemove(licenseesToRemove)
              .build())
      ));
    }

    return changes;
  }

  /**
   * One consortium entry applied to the licensees held.
   */
  private static void apply(
      PearsOperation operation,
      PearsOperation.ConsortiumEntry entry,
      Set<Integer> licenseeIds,
      MigrationNotes notes
  ) {
    var operationId = operation.header().operationId();

    if (entry.type() == null) {
      notes.unmapped("licensee entry with no ENTRY_TYPE", operationId);
      return;
    }

    switch (entry.type()) {
      case SET -> {
        var joiningId = licenseeId(entry.primary(), "joining", operationId, notes);
        if (joiningId != null) {
          join(joiningId, licenseeIds, notes);
        }
      }
      case REMOVE -> {
        var withdrawingId = licenseeId(entry.primary(), "withdrawing", operationId, notes);
        if (withdrawingId != null) {
          withdraw(withdrawingId, licenseeIds, notes);
        }
      }
      case TRANSFER -> {
        var withdrawingId = licenseeId(entry.primary(), "withdrawing", operationId, notes);
        var joiningId = licenseeId(entry.secondary(), "joining", operationId, notes);
        // Half a transfer would join a licensee without the one it replaced leaving, or the other
        // way around, so it is carried whole or not at all.
        if (withdrawingId != null && joiningId != null) {
          withdraw(withdrawingId, licenseeIds, notes);
          join(joiningId, licenseeIds, notes);
        }
      }
      default -> throw new IllegalStateException("Unexpected entry type %s".formatted(entry.type()));
    }
  }

  private static void join(int licenseeId, Set<Integer> licenseeIds, MigrationNotes notes) {
    if (!licenseeIds.add(licenseeId)) {
      notes.note("licensee joining a licence it already holds");
    }
  }

  private static void withdraw(int licenseeId, Set<Integer> licenseeIds, MigrationNotes notes) {
    if (!licenseeIds.remove(licenseeId)) {
      notes.note("licensee withdrawing from a licence it does not hold");
    }
  }

  /**
   * The organisation an entry names, or null where it names none.
   */
  private static Integer licenseeId(
      PearsOperation.Organisation organisation,
      String side,
      long operationId,
      MigrationNotes notes
  ) {
    if (organisation == null) {
      notes.unmapped("licensee entry with no %s organisation".formatted(side), operationId);
      return null;
    }

    return ConsortiumLists.organisationUnitId(organisation);
  }

  private static boolean isLicenseeList(PearsOperation operation) {
    return ConsortiumLists.isList(operation, PearsCompanyListType.LICENSEE);
  }
}
