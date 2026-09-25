package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * One licence rebuilt from PEARS, as the migration itself sees it: the history it was built from,
 * and which position was built for which PEARS transaction.
 *
 * @param notesByMigrator         what each migrator decided and could not carry, by {@code name()}
 * @param includedOperationsByType the PEARS operations some migrator supports, by OPERATION_TYPE
 * @param ignoredOperationsByType the PEARS operations no migrator supports, by OPERATION_TYPE
 */
record LicenceWriteback(
    String result,
    PearsLicenceHistory history,
    Map<Long, LicencePosition> positionsByTransactionId,
    Map<String, MigrationNotes> notesByMigrator,
    Map<String, Integer> includedOperationsByType,
    Map<String, Integer> ignoredOperationsByType,
    int changes,
    int operations
) {

  /**
   * A licence nothing was built for.
   */
  static LicenceWriteback nothingToDo(String result, PearsLicenceHistory history) {
    return new LicenceWriteback(
        result,
        history,
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        0,
        0
    );
  }

  int operationsIgnored() {
    return ignoredOperationsByType.values().stream().mapToInt(Integer::intValue).sum();
  }

  LicenceWritebackResult toResult() {
    return new LicenceWritebackResult(
        result,
        positionsByTransactionId.size(),
        changes,
        operations,
        operationsIgnored()
    );
  }
}
