package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigratedChange;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.PearsOperationMigrator;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.PearsPositionKey;

/**
 * Rebuilds a licence in this application out of the history PEARS holds for it, replaying it
 * through the same services the application uses to build a position itself. What each position
 * holds is the registered {@link PearsOperationMigrator}s' business; this decides only where their
 * changes go.
 *
 * <p>Every position PEARS holds is built, the ones the licence starts and ends at included. An
 * operation no migrator supports is ignored: its position is built, carrying nothing it did.
 */
@Service
@ConditionalOnPearsDataSource
class LicenceWritebackService {

  private final LicenceTransactionService licenceTransactionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final LicenceCleardownService licenceCleardownService;
  private final PearsLicenceService pearsLicenceService;
  private final List<PearsOperationMigrator> migrators;

  LicenceWritebackService(
      LicenceTransactionService licenceTransactionService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      LicenceCleardownService licenceCleardownService,
      PearsLicenceService pearsLicenceService,
      List<PearsOperationMigrator> migrators
  ) {
    this.licenceTransactionService = licenceTransactionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.licenceCleardownService = licenceCleardownService;
    this.pearsLicenceService = pearsLicenceService;
    this.migrators = migrators;
  }

  @Transactional
  LicenceWriteback overwriteLicencePositionsFromPears(Licence licence) {
    var operationHistory = pearsLicenceService.licenceHistory(
        licence.getPrefix(),
        Integer.parseInt(licence.getLicenceNumber()),
        wantedOperationTypes()
    );

    var history = PearsLicenceHistory.reconstruct(operationHistory);

    // A history that arrives empty is as likely to be the query as the licence, so it does not
    // clear the licence.
    if (history.positions().isEmpty()) {
      return LicenceWriteback.nothingToDo(
          "No positions found in PEARS for licence %s".formatted(licence.getLicenceReference()), history);
    }

    var notesByMigrator = new LinkedHashMap<String, MigrationNotes>();
    var changesByPosition = produceChanges(history, notesByMigrator);
    var includedOperationsByType = operationsByType(history, true);
    var ignoredOperationsByType = operationsByType(history, false);

    licenceCleardownService.clear(licence);

    var positionsByTransactionId = new LinkedHashMap<Long, LicencePosition>();
    var changesSaved = 0;
    var operationsSaved = 0;

    for (var pearsPosition : history.positions()) {
      var licenceTransaction = licenceTransactionService.createLicenceTransaction(pearsPosition.regulatorReference());
      var licencePosition = licencePositionService.createLicencePosition(
          licence,
          licenceTransaction,
          pearsPosition.positionDate()
      );
      positionsByTransactionId.put(pearsPosition.transactionId(), licencePosition);

      var changeOrder = 0;
      for (var change : changesByPosition.getOrDefault(new PearsPositionKey(pearsPosition.transactionId()), List.of())) {
        licencePositionChangeService.createLicencePositionChange(
            licencePosition,
            change.migratedChange().operations(),
            ++changeOrder,
            change.migratedChange().status()
        );
        changesSaved++;
        operationsSaved += change.migratedChange().operations().size();
      }
    }

    return new LicenceWriteback(
        "Saved %d positions, %d changes and %d operations for licence %s".formatted(
            positionsByTransactionId.size(), changesSaved, operationsSaved, history.licenceReference()),
        history,
        positionsByTransactionId,
        notesByMigrator,
        includedOperationsByType,
        ignoredOperationsByType,
        changesSaved,
        operationsSaved
    );
  }

  /**
   * The PEARS operations some migrator supports, or the ones none does, counted by OPERATION_TYPE.
   * The positions of the unsupported ones are still built -- a licence holds every position PEARS
   * holds -- but what they did is not carried across.
   */
  private Map<String, Integer> operationsByType(PearsLicenceHistory history, boolean supported) {
    var operationsByType = new TreeMap<String, Integer>();
    for (var position : history.positions()) {
      for (var operation : position.operations()) {
        if (migrators.stream().anyMatch(migrator -> migrator.supports(operation)) == supported) {
          operationsByType.merge(String.valueOf(operation.typeName()), 1, Integer::sum);
        }
      }
    }
    return operationsByType;
  }

  /**
   * Every operation type some migrator needs the XML of, which is what
   * {@code licence-history.sql} fetches a payload for.
   */
  Set<String> wantedOperationTypes() {
    return migrators.stream()
        .flatMap(migrator -> migrator.pearsOperationTypes().stream())
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * What every migrator makes of the licence, grouped by the position it lands on. Ordered by PEARS'
   * order first, then by migrator and emission order so the result is stable between runs.
   */
  private LinkedHashMap<PearsPositionKey, List<OrderedChange>> produceChanges(
      PearsLicenceHistory history,
      LinkedHashMap<String, MigrationNotes> notesByMigrator
  ) {
    var orderedChanges = new ArrayList<OrderedChange>();

    for (var migratorIndex = 0; migratorIndex < migrators.size(); migratorIndex++) {
      var migrator = migrators.get(migratorIndex);
      var notes = notesByMigrator.computeIfAbsent(migrator.name(), name -> new MigrationNotes());

      List<MigratedChange> changes = migrator.migrate(history, notes);
      for (var emissionIndex = 0; emissionIndex < changes.size(); emissionIndex++) {
        orderedChanges.add(new OrderedChange(changes.get(emissionIndex), migratorIndex, emissionIndex));
      }
    }

    orderedChanges.sort(Comparator
        .comparing((OrderedChange change) -> change.migratedChange().at())
        .thenComparingInt(OrderedChange::migratorIndex)
        .thenComparingInt(OrderedChange::emissionIndex));

    var changesByPosition = new LinkedHashMap<PearsPositionKey, List<OrderedChange>>();
    for (var change : orderedChanges) {
      changesByPosition
          .computeIfAbsent(change.migratedChange().position(), position -> new ArrayList<>())
          .add(change);
    }
    return changesByPosition;
  }

  /**
   * A produced change and the two things that settle its order where PEARS' own order does not.
   */
  private record OrderedChange(MigratedChange migratedChange, int migratorIndex, int emissionIndex) {
  }
}
