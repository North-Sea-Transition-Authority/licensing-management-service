package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Dry runs the migration of one licence: migrates it as a run would, compares every version with PEARS, and rolls the
 * whole of it back, so the environment is checked without being changed (plan, design section 5).
 */
@Service
@Profile("migration")
@ConditionalOnPearsDataSource
public class PearsScheduleValidationService {

  static final String COMPARED_AS_IT_IS = "Already had a schedule in LMS: compared as it is";
  // Prefixes the comparison result counts in the notes, which hold the mapper's notes too
  public static final String RESULT_NOTE_PREFIX = "Result: ";

  // Enough to start on: the comparison report shows the rest
  static final int ISSUES_KEPT = 20;

  private final PearsScheduleMigrationService migrationService;
  private final PearsScheduleComparisonService comparisonService;
  private final TransactionTemplate rolledBackTransaction;

  PearsScheduleValidationService(
      PearsScheduleMigrationService migrationService,
      PearsScheduleComparisonService comparisonService,
      PlatformTransactionManager transactionManager
  ) {
    this.migrationService = migrationService;
    this.comparisonService = comparisonService;
    // A transaction of its own, so what the migration writes is rolled back even if the caller is in one
    this.rolledBackTransaction = new TransactionTemplate(transactionManager);
    this.rolledBackTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Dry runs one licence. A licence that already has an LMS schedule, for example from an earlier migration, is
   * compared as it is.
   *
   * @param eventTypes the PEARS event type catalogue, read once for the run
   * @param runId      the dry run, recorded against the licence as a migration run would be, then rolled back
   */
  public PearsScheduleValidationOutcome validate(
      String licenceReference,
      List<PearsScheduleEventType> eventTypes,
      UUID runId
  ) {
    var reference = PearsLicenceReference.parse(licenceReference);
    return rolledBackTransaction.execute(status -> {
      status.setRollbackOnly();
      return migrateAndCompare(reference, eventTypes, runId);
    });
  }

  private PearsScheduleValidationOutcome migrateAndCompare(
      PearsLicenceReference reference,
      List<PearsScheduleEventType> eventTypes,
      UUID runId
  ) {
    var migration = migrationService.migrateLicence(reference, eventTypes, runId);
    var notes = new LinkedHashMap<>(migration.notes().noteCountsByReason());

    var alreadyHadASchedule = migration.result() == PearsScheduleMigrationOutcome.Result.SKIPPED
        && PearsScheduleMigrationService.ALREADY_HAS_A_SCHEDULE.equals(migration.reason());
    if (alreadyHadASchedule) {
      notes.put(COMPARED_AS_IT_IS, 1);
    } else if (migration.result() != PearsScheduleMigrationOutcome.Result.MIGRATED) {
      var result = migration.result() == PearsScheduleMigrationOutcome.Result.SKIPPED
          ? PearsScheduleValidationOutcome.Result.SKIPPED
          : PearsScheduleValidationOutcome.Result.FAILED;
      return new PearsScheduleValidationOutcome(result, migration.reason(), notes);
    }

    var comparison = comparisonService.compare(reference, eventTypes, true);
    comparison.resultCounts().forEach((result, count) -> notes.put(RESULT_NOTE_PREFIX + result.getText(), count));

    if (!comparison.hasIssues()) {
      return new PearsScheduleValidationOutcome(PearsScheduleValidationOutcome.Result.MATCHED, null, notes);
    }
    return new PearsScheduleValidationOutcome(
        PearsScheduleValidationOutcome.Result.DIFFERS, kept(comparison.issueDescriptions()), notes);
  }

  private static String kept(List<String> issues) {
    if (issues.size() <= ISSUES_KEPT) {
      return String.join(System.lineSeparator(), issues);
    }
    var kept = new ArrayList<>(issues.subList(0, ISSUES_KEPT));
    kept.add("... (+%,d more)".formatted(issues.size() - ISSUES_KEPT));
    return String.join(System.lineSeparator(), kept);
  }
}
