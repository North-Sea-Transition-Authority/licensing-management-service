package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationService;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * The worker over the real run tables, with PEARS and the migration of each licence stubbed.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class PearsScheduleMigrationJobIntegrationTest {

  private static final List<PearsScheduleEventType> EVENT_TYPES =
      List.of(new PearsScheduleEventType("T", "Term", -3));

  @Autowired
  private PearsScheduleMigrationRunService runService;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private Clock clock;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private PearsScheduleMigrationService migrationService;
  private PearsScheduleService pearsScheduleService;
  private PearsScheduleCleardownService cleardownService;
  private PearsScheduleValidationService validationService;

  @BeforeEach
  void setUp() {
    migrationService = mock(PearsScheduleMigrationService.class);
    pearsScheduleService = mock(PearsScheduleService.class);
    cleardownService = mock(PearsScheduleCleardownService.class);
    validationService = mock(PearsScheduleValidationService.class);
    when(pearsScheduleService.eventTypes()).thenReturn(EVENT_TYPES);
    when(migrationService.migrateLicence(any(), any(), any()))
        .thenAnswer(invocation -> migrated(invocation.getArgument(0)));
  }

  @Test
  void workNextBatch_migratesEachLicenceInTheBatchAndRecordsWhatBecameOfIt() {
    var run = runService.startRun(List.of("P1", "PL116", "P3"), 1L);
    var notes = new MigrationNotes();
    notes.note("Default description");
    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 1)), any(), any()))
        .thenReturn(new PearsScheduleMigrationOutcome(
            new PearsLicenceReference("P", 1), PearsScheduleMigrationOutcome.Result.MIGRATED, null, notes));
    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 3)), any(), any()))
        .thenReturn(new PearsScheduleMigrationOutcome(
            new PearsLicenceReference("P", 3), PearsScheduleMigrationOutcome.Result.SKIPPED, "Not a licence in LMS",
            new MigrationNotes()));

    job(2).workNextBatch();

    verify(migrationService).migrateLicence(new PearsLicenceReference("P", 1), EVENT_TYPES, run.getId());
    verify(migrationService).migrateLicence(new PearsLicenceReference("P", 3), EVENT_TYPES, run.getId());
    verify(migrationService, never()).migrateLicence(eq(new PearsLicenceReference("PL", 116)), any(), any());
    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.MIGRATED),
        entry("P3", PearsScheduleMigrationQueueStatus.SKIPPED),
        entry("PL116", PearsScheduleMigrationQueueStatus.PENDING)
    );
    assertThat(queueEntry(run.getId(), "P1").getNotes()).containsExactly(entry("Default description", 1));
    assertThat(queueEntry(run.getId(), "P3").getOutcome()).isEqualTo("Not a licence in LMS");
  }

  @Test
  void workNextBatch_onceEveryLicenceHasAnOutcome_completesTheRun() {
    var run = runService.startRun(List.of("P1", "P2", "P3"), 1L);
    var job = job(2);

    job.workNextBatch();
    job.workNextBatch();
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.RUNNING);
    job.workNextBatch();

    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);
    assertThat(statuses(run.getId())).containsOnlyKeys("P1", "P2", "P3")
        .allSatisfy((licence, status) -> assertThat(status).isEqualTo(PearsScheduleMigrationQueueStatus.MIGRATED));
  }

  @Test
  void workNextBatch_whenAnInstanceStopsMidLicence_theNextPassCarriesOnFromThatLicence() {
    var run = runService.startRun(List.of("P1", "P2", "P3"), 1L);
    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 2)), any(), any()))
        .thenThrow(new InstanceStopped());

    var stoppingJob = job(10);
    assertThatThrownBy(stoppingJob::workNextBatch).isInstanceOf(InstanceStopped.class);
    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.MIGRATED),
        entry("P2", PearsScheduleMigrationQueueStatus.PENDING),
        entry("P3", PearsScheduleMigrationQueueStatus.PENDING)
    );

    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 2)), any(), any()))
        .thenReturn(migrated(new PearsLicenceReference("P", 2)));
    var restartedJob = job(10);
    restartedJob.workNextBatch();
    restartedJob.workNextBatch();

    verify(migrationService).migrateLicence(new PearsLicenceReference("P", 1), EVENT_TYPES, run.getId());
    assertThat(statuses(run.getId())).containsOnlyKeys("P1", "P2", "P3")
        .allSatisfy((licence, status) -> assertThat(status).isEqualTo(PearsScheduleMigrationQueueStatus.MIGRATED));
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);
  }

  @Test
  void workNextBatch_whenAMigrationThrows_recordsTheLicenceAsFailedAndCarriesOn() {
    var run = runService.startRun(List.of("P1", "P2"), 1L);
    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 1)), any(), any()))
        .thenThrow(new IllegalStateException("PEARS went away"));

    job(10).workNextBatch();

    var failed = queueEntry(run.getId(), "P1");
    assertThat(failed.getStatus()).isEqualTo(PearsScheduleMigrationQueueStatus.FAILED);
    assertThat(failed.getOutcome()).isEqualTo("Could not be migrated: PEARS went away");
    assertThat(failed.getAttempts()).isEqualTo(1);
    assertThat(queueEntry(run.getId(), "P2").getStatus()).isEqualTo(PearsScheduleMigrationQueueStatus.MIGRATED);
  }

  @Test
  void workNextBatch_whenTheRunIsCancelledMidBatch_finishesTheLicenceItIsOnAndStops() {
    var run = runService.startRun(List.of("P1", "P2"), 1L);
    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 1)), any(), any()))
        .thenAnswer(invocation -> {
          runService.cancel(run.getId());
          return migrated(invocation.getArgument(0));
        });

    job(10).workNextBatch();

    verify(migrationService, never()).migrateLicence(eq(new PearsLicenceReference("P", 2)), any(), any());
    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.MIGRATED),
        entry("P2", PearsScheduleMigrationQueueStatus.PENDING)
    );
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.CANCELLED);
  }

  @Test
  void workNextBatch_withNoActiveRun_readsNothingFromPears() {
    job(10).workNextBatch();

    verifyNoInteractions(pearsScheduleService, migrationService);
  }

  @Test
  void workNextBatch_whenAFailedLicenceIsRetried_migratesItOnTheNextPass() {
    var run = runService.startRun(List.of("P1"), 1L);
    when(migrationService.migrateLicence(eq(new PearsLicenceReference("P", 1)), any(), any()))
        .thenThrow(new IllegalStateException("PEARS went away"))
        .thenReturn(migrated(new PearsLicenceReference("P", 1)));
    var job = job(10);
    job.workNextBatch();
    job.workNextBatch();
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);

    runService.retryFailed(run.getId());
    entityManager.clear();
    job.workNextBatch();

    var retried = queueEntry(run.getId(), "P1");
    assertThat(retried.getStatus()).isEqualTo(PearsScheduleMigrationQueueStatus.MIGRATED);
    assertThat(retried.getAttempts()).isEqualTo(2);
  }

  @Test
  void construct_withABatchSizeUnderOne_isRefused() {
    assertThatThrownBy(() -> job(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pears.schedule-migration.batch-size must be at least 1");
  }

  @Test
  void workNextBatch_forACleardown_clearsEachLicenceWithoutReadingPears() {
    var run = runService.startCleardown(List.of("P1", "P2"), 1L);
    when(cleardownService.clear("P1"))
        .thenReturn(new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.CLEARED, null));
    when(cleardownService.clear("P2")).thenReturn(new PearsScheduleCleardownOutcome(
        PearsScheduleCleardownOutcome.Result.SKIPPED, "Has no schedule from the PEARS migration"));

    job(10).workNextBatch();

    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.CLEARED),
        entry("P2", PearsScheduleMigrationQueueStatus.SKIPPED)
    );
    assertThat(queueEntry(run.getId(), "P2").getOutcome()).isEqualTo("Has no schedule from the PEARS migration");
    verifyNoInteractions(pearsScheduleService, migrationService);
  }

  @Test
  void workNextBatch_whenALicenceCannotBeCleared_recordsItAsFailedAndCarriesOn() {
    var run = runService.startCleardown(List.of("P1", "P2"), 1L);
    when(cleardownService.clear("P1")).thenThrow(new IllegalStateException("Foreign key violation"));
    when(cleardownService.clear("P2"))
        .thenReturn(new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.CLEARED, null));

    job(10).workNextBatch();

    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.FAILED),
        entry("P2", PearsScheduleMigrationQueueStatus.CLEARED)
    );
    assertThat(queueEntry(run.getId(), "P1").getOutcome()).isEqualTo("Could not be cleared: Foreign key violation");
  }

  @Test
  void workNextBatch_whenADryRun_recordsWhatEachLicenceWouldBeAndTotalsItsComparisonResults() {
    var run = runService.startValidation(List.of("P1", "P2", "P3"), 1L);
    var issue = "version 2, Rate In Second Term: PEARS 2018-12-01 150, LMS 2018-12-01 175: Value differs";
    when(validationService.validate("P1", EVENT_TYPES, run.getId())).thenReturn(new PearsScheduleValidationOutcome(
        PearsScheduleValidationOutcome.Result.MATCHED, null, Map.of("Result: Match", 9, "Default description", 1)));
    when(validationService.validate("P2", EVENT_TYPES, run.getId())).thenReturn(new PearsScheduleValidationOutcome(
        PearsScheduleValidationOutcome.Result.DIFFERS, issue, Map.of("Result: Match", 4, "Result: Value differs", 1)));
    when(validationService.validate("P3", EVENT_TYPES, run.getId())).thenThrow(new IllegalStateException("PEARS went away"));

    job(10).workNextBatch();

    verify(migrationService, never()).migrateLicence(any(), any(), any());
    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.MATCHED),
        entry("P2", PearsScheduleMigrationQueueStatus.DIFFERS),
        entry("P3", PearsScheduleMigrationQueueStatus.FAILED)
    );
    var report = runService.report(run.getId()).orElseThrow();
    assertThat(report.kind()).isEqualTo(PearsScheduleMigrationRunKind.VALIDATE);
    assertThat(report.withIssues()).containsExactly(new PearsScheduleMigrationRunReport.LicenceOutcome("P2", issue, 1));
    assertThat(report.failed()).containsExactly(
        new PearsScheduleMigrationRunReport.LicenceOutcome("P3", "Could not be dry run: PEARS went away", 1));
    assertThat(report.comparisonResults()).containsExactly(entry("Match", 13L), entry("Value differs", 1L));
    assertThat(runService.licences(run.getId())).hasValueSatisfying(licences -> assertThat(licences)
        .extracting(PearsScheduleMigrationRunService.RunLicence::licenceReference,
            PearsScheduleMigrationRunService.RunLicence::status, PearsScheduleMigrationRunService.RunLicence::outcome)
        .containsExactly(
            tuple("P1", PearsScheduleMigrationQueueStatus.MATCHED, null),
            tuple("P2", PearsScheduleMigrationQueueStatus.DIFFERS, issue),
            tuple("P3", PearsScheduleMigrationQueueStatus.FAILED, "Could not be dry run: PEARS went away")
        ));
    assertThat(runService.licences(UUID.randomUUID())).isEmpty();
  }

  private PearsScheduleMigrationJob job(int batchSize) {
    return new PearsScheduleMigrationJob(
        runService, migrationService, pearsScheduleService, cleardownService, validationService, clock, batchSize);
  }

  private static PearsScheduleMigrationOutcome migrated(PearsLicenceReference licence) {
    return new PearsScheduleMigrationOutcome(
        licence, PearsScheduleMigrationOutcome.Result.MIGRATED, null, new MigrationNotes());
  }

  private PearsScheduleMigrationQueueEntry queueEntry(UUID runId, String licenceReference) {
    return entityManager.find(
        PearsScheduleMigrationQueueEntry.class, new PearsScheduleMigrationQueueEntryId(runId, licenceReference));
  }

  private Map<String, PearsScheduleMigrationQueueStatus> statuses(UUID runId) {
    var statuses = new TreeMap<String, PearsScheduleMigrationQueueStatus>();
    entityManager.createQuery(
            "SELECT e FROM PearsScheduleMigrationQueueEntry e WHERE e.id.runId = :runId",
            PearsScheduleMigrationQueueEntry.class)
        .setParameter("runId", runId)
        .getResultList()
        .forEach(queued -> statuses.put(queued.getId().licenceReference(), queued.getStatus()));
    return statuses;
  }

  /**
   * Stands in for the instance stopping part way through a licence, which the worker doesn't catch.
   */
  private static class InstanceStopped extends Error {
  }
}
