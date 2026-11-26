package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationService;

/**
 * Dry runs the work programme migration of one licence: migrates it onto its migrated schedule as a run would, compares
 * it with PEARS, and rolls the whole of it back, so the environment is checked without being changed (plan, design
 * section 5).
 */
@Service
@Profile("migration")
@ConditionalOnPearsDataSource
public class PearsWorkProgrammeValidationService {

  static final String COMPARED_AS_IT_IS = "Work programme already migrated: compared as it is";

  private final PearsWorkProgrammeMigrationService migrationService;
  private final PearsWorkProgrammeComparisonService comparisonService;
  private final TransactionTemplate rolledBackTransaction;

  PearsWorkProgrammeValidationService(
      PearsWorkProgrammeMigrationService migrationService,
      PearsWorkProgrammeComparisonService comparisonService,
      PlatformTransactionManager transactionManager
  ) {
    this.migrationService = migrationService;
    this.comparisonService = comparisonService;
    // A transaction of its own, so what the migration writes is rolled back even if the caller is in one
    this.rolledBackTransaction = new TransactionTemplate(transactionManager);
    this.rolledBackTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Dry runs one licence. A licence whose work programme is already migrated is compared as it is.
   *
   * @param runId the dry run, recorded against the licence as a migration run would be, then rolled back
   */
  public PearsScheduleValidationOutcome validate(String licenceReference, UUID runId) {
    var reference = PearsLicenceReference.parse(licenceReference);
    return rolledBackTransaction.execute(status -> {
      status.setRollbackOnly();
      return migrateAndCompare(reference, runId);
    });
  }

  private PearsScheduleValidationOutcome migrateAndCompare(PearsLicenceReference reference, UUID runId) {
    var migration = migrationService.migrateLicence(reference, runId);
    var notes = new LinkedHashMap<>(migration.notes().noteCountsByReason());

    var alreadyMigrated = migration.result() == PearsWorkProgrammeMigrationOutcome.Result.SKIPPED
        && PearsWorkProgrammeMigrationService.ALREADY_MIGRATED.equals(migration.reason());
    if (alreadyMigrated) {
      notes.put(COMPARED_AS_IT_IS, 1);
    } else if (migration.result() != PearsWorkProgrammeMigrationOutcome.Result.MIGRATED) {
      var result = migration.result() == PearsWorkProgrammeMigrationOutcome.Result.SKIPPED
          ? PearsScheduleValidationOutcome.Result.SKIPPED
          : PearsScheduleValidationOutcome.Result.FAILED;
      return new PearsScheduleValidationOutcome(result, migration.reason(), notes);
    }

    var comparison = comparisonService.compare(reference);
    comparison.resultCounts().forEach((result, count) ->
        notes.put(PearsScheduleValidationService.RESULT_NOTE_PREFIX + result.getText(), count));

    if (!comparison.hasIssues()) {
      return new PearsScheduleValidationOutcome(PearsScheduleValidationOutcome.Result.MATCHED, null, notes);
    }
    return new PearsScheduleValidationOutcome(PearsScheduleValidationOutcome.Result.DIFFERS,
        PearsScheduleValidationService.keptIssues(comparison.issueDescriptions()), notes);
  }
}
