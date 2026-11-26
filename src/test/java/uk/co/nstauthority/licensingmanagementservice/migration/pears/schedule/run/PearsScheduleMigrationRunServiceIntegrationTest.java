package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class PearsScheduleMigrationRunServiceIntegrationTest {

  private static final long WUA_ID = 1001L;

  @Autowired
  private PearsScheduleMigrationRunService runService;

  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  @Test
  void startRun_queuesEachLicenceOnceAsPending() {
    var run = runService.startRun(List.of("P2244", "P1161", "P2244"), WUA_ID);

    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.QUEUED);
    assertThat(run.getKind()).isEqualTo(PearsScheduleMigrationRunKind.MIGRATE);
    assertThat(run.getRequestedBy()).isEqualTo(WUA_ID);
    assertThat(run.getRequestedAt()).isNotNull();
    assertThat(statuses(run.getId())).containsExactly(
        entry("P1161", PearsScheduleMigrationQueueStatus.PENDING),
        entry("P2244", PearsScheduleMigrationQueueStatus.PENDING)
    );
  }

  @Test
  void startRun_whenARunIsActive_isRefused() {
    var run = runService.startRun(List.of("P2244"), WUA_ID);

    var anotherLicence = List.of("P1161");

    assertThatThrownBy(() -> runService.startRun(anotherLicence, WUA_ID))
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessageContaining(run.getId().toString());
  }

  @Test
  void startRun_whenTheLastRunHasFinished_queuesANewRun() {
    var first = runService.startRun(List.of("P2244"), WUA_ID);
    runService.cancel(first.getId());

    var second = runService.startRun(List.of("P2244"), WUA_ID);

    assertThat(second.getId()).isNotEqualTo(first.getId());
  }

  @Test
  void nextBatch_startsTheRunAndTakesThePendingLicencesInOrder() {
    var run = runService.startRun(List.of("P3", "P1", "P2"), WUA_ID);

    var batch = runService.nextBatch(2);

    assertThat(batch).contains(new PearsScheduleMigrationRunService.Batch(
        run.getId(), PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.MIGRATE, List.of("P1", "P2")));
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.RUNNING);
  }

  @Test
  void nextBatch_skipsLicencesWithAnOutcome() {
    var run = runService.startRun(List.of("P1", "P2"), WUA_ID);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.MIGRATED, null);

    assertThat(runService.nextBatch(2))
        .contains(new PearsScheduleMigrationRunService.Batch(
            run.getId(), PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.MIGRATE, List.of("P2")));
  }

  @Test
  void nextBatch_whenNothingIsPending_completesTheRun() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    runService.nextBatch(10);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.MIGRATED, null);

    assertThat(runService.nextBatch(10)).isEmpty();
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);
    assertThat(run.getCompletedAt()).isNotNull();
  }

  @Test
  void nextBatch_withNoActiveRun_isEmpty() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    runService.cancel(run.getId());

    assertThat(runService.nextBatch(10)).isEmpty();
  }

  @Test
  void isActive_isFalseOnceCancelled() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    assertThat(runService.isActive(run.getId())).isTrue();

    runService.cancel(run.getId());

    assertThat(runService.isActive(run.getId())).isFalse();
    assertThat(runService.isActive(UUID.randomUUID())).isFalse();
  }

  @Test
  void recordOutcome_recordsTheResultNotesAndTimings() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    var startedAt = Instant.parse("2026-11-18T09:00:00Z");

    var recorded = runService.recordOutcome(
        run.getId(), "P1", PearsScheduleMigrationQueueStatus.SKIPPED, "Already has a schedule in LMS",
        Map.of("Default description", 2), startedAt);

    assertThat(recorded).isTrue();
    var entry = queueEntry(run.getId(), "P1");
    assertThat(entry.getStatus()).isEqualTo(PearsScheduleMigrationQueueStatus.SKIPPED);
    assertThat(entry.getOutcome()).isEqualTo("Already has a schedule in LMS");
    assertThat(entry.getNotes()).containsExactly(entry("Default description", 2));
    assertThat(entry.getAttempts()).isEqualTo(1);
    assertThat(entry.getStartedAt()).isEqualTo(startedAt);
    assertThat(entry.getFinishedAt()).isNotNull();
  }

  @Test
  void recordOutcome_withNoNotes_leavesTheNotesEmpty() {
    var run = runService.startRun(List.of("P1"), WUA_ID);

    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.MIGRATED, null);

    assertThat(queueEntry(run.getId(), "P1").getNotes()).isNull();
  }

  @Test
  void recordOutcome_whenAnotherWorkerGotThereFirst_keepsTheFirstOutcome() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.MIGRATED, null);

    var recorded = giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.SKIPPED, "Already has a schedule in LMS");

    assertThat(recorded).isFalse();
    var entry = queueEntry(run.getId(), "P1");
    assertThat(entry.getStatus()).isEqualTo(PearsScheduleMigrationQueueStatus.MIGRATED);
    assertThat(entry.getAttempts()).isEqualTo(1);
  }

  @Test
  void cancel_cancelsTheRun() {
    var run = runService.startRun(List.of("P1"), WUA_ID);

    var report = runService.cancel(run.getId());

    assertThat(report).hasValueSatisfying(cancelled -> {
      assertThat(cancelled.status()).isEqualTo(PearsScheduleMigrationRunStatus.CANCELLED);
      assertThat(cancelled.completedAt()).isNotNull();
    });
  }

  @Test
  void cancel_whenTheRunHasFinished_isRefused() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    runService.cancel(run.getId());

    var runId = run.getId();

    assertThatThrownBy(() -> runService.cancel(runId))
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessage("Run %s is already CANCELLED".formatted(run.getId()));
  }

  @Test
  void cancel_whenTheRunIsUnknown_isEmpty() {
    assertThat(runService.cancel(UUID.randomUUID())).isEmpty();
  }

  @Test
  void retryFailed_queuesTheFailedLicencesAgainAndReopensTheRun() {
    var run = runService.startRun(List.of("P1", "P2"), WUA_ID);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.FAILED, "Could not be written");
    giveOutcome(run.getId(), "P2", PearsScheduleMigrationQueueStatus.MIGRATED, null);
    runService.nextBatch(10);
    assertThat(run.getStatus()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);

    var report = runService.retryFailed(run.getId());

    assertThat(report).hasValueSatisfying(retried -> {
      assertThat(retried.status()).isEqualTo(PearsScheduleMigrationRunStatus.RUNNING);
      assertThat(retried.completedAt()).isNull();
    });
    entityManager.clear();
    assertThat(statuses(run.getId())).containsExactly(
        entry("P1", PearsScheduleMigrationQueueStatus.PENDING),
        entry("P2", PearsScheduleMigrationQueueStatus.MIGRATED)
    );
    // The failure is cleared until the retry records its own, but the attempt still counts
    var retried = queueEntry(run.getId(), "P1");
    assertThat(retried.getOutcome()).isNull();
    assertThat(retried.getNotes()).isNull();
    assertThat(retried.getStartedAt()).isNull();
    assertThat(retried.getFinishedAt()).isNull();
    assertThat(retried.getAttempts()).isEqualTo(1);
    assertThat(runService.nextBatch(10))
        .contains(new PearsScheduleMigrationRunService.Batch(
            run.getId(), PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.MIGRATE, List.of("P1")));
  }

  @Test
  void retryFailed_withNothingFailed_leavesACompletedRunComplete() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.MIGRATED, null);
    runService.nextBatch(10);

    assertThat(runService.retryFailed(run.getId()))
        .hasValueSatisfying(report -> assertThat(report.status()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE));
  }

  @Test
  void retryFailed_whenTheRunWasCancelled_isRefused() {
    var run = runService.startRun(List.of("P1"), WUA_ID);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.FAILED, "Could not be written");
    runService.cancel(run.getId());

    var runId = run.getId();

    assertThatThrownBy(() -> runService.retryFailed(runId))
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessage("Run %s was cancelled, so start a new run instead".formatted(run.getId()));
  }

  @Test
  void retryFailed_whenAnotherRunIsActive_isRefused() {
    var first = runService.startRun(List.of("P1"), WUA_ID);
    giveOutcome(first.getId(), "P1", PearsScheduleMigrationQueueStatus.FAILED, "Could not be written");
    runService.nextBatch(10);
    var second = runService.startRun(List.of("P2"), WUA_ID);

    var firstRunId = first.getId();

    assertThatThrownBy(() -> runService.retryFailed(firstRunId))
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessageContaining(second.getId().toString());
  }

  @Test
  void report_countsTheLicencesAndListsThoseSkippedOrFailed() {
    var run = runService.startRun(List.of("P1", "P2", "P3", "P4"), WUA_ID);
    giveOutcome(run.getId(), "P1", PearsScheduleMigrationQueueStatus.MIGRATED, null);
    giveOutcome(run.getId(), "P2", PearsScheduleMigrationQueueStatus.SKIPPED, "Not a licence in LMS");
    giveOutcome(run.getId(), "P3", PearsScheduleMigrationQueueStatus.FAILED, "Could not be written");

    var report = runService.report(run.getId());

    assertThat(report).hasValueSatisfying(found -> {
      assertThat(found.runId()).isEqualTo(run.getId());
      assertThat(found.licencesByStatus()).containsExactly(
          entry(PearsScheduleMigrationQueueStatus.PENDING, 1L),
          entry(PearsScheduleMigrationQueueStatus.MIGRATED, 1L),
          entry(PearsScheduleMigrationQueueStatus.CLEARED, 0L),
          entry(PearsScheduleMigrationQueueStatus.MATCHED, 0L),
          entry(PearsScheduleMigrationQueueStatus.DIFFERS, 0L),
          entry(PearsScheduleMigrationQueueStatus.SKIPPED, 1L),
          entry(PearsScheduleMigrationQueueStatus.FAILED, 1L)
      );
      assertThat(found.skipped())
          .containsExactly(new PearsScheduleMigrationRunReport.LicenceOutcome("P2", "Not a licence in LMS", 1));
      assertThat(found.failed())
          .containsExactly(new PearsScheduleMigrationRunReport.LicenceOutcome("P3", "Could not be written", 1));
      // Only a dry run has these
      assertThat(found.withIssues()).isEmpty();
      assertThat(found.comparisonResults()).isEmpty();
    });
    assertThat(runService.report(UUID.randomUUID())).isEmpty();
  }

  @Test
  void startCleardown_queuesACleardownRun() {
    var run = runService.startCleardown(List.of("P2244"), WUA_ID);

    assertThat(run.getKind()).isEqualTo(PearsScheduleMigrationRunKind.CLEARDOWN);
    assertThat(statuses(run.getId())).containsExactly(entry("P2244", PearsScheduleMigrationQueueStatus.PENDING));
    assertThat(runService.nextBatch(10)).contains(new PearsScheduleMigrationRunService.Batch(
        run.getId(), PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.CLEARDOWN, List.of("P2244")));
  }

  @Test
  void startCleardown_whenARunIsActive_isRefused() {
    runService.startRun(List.of("P2244"), WUA_ID);

    var licences = List.of("P2244");

    assertThatThrownBy(() -> runService.startCleardown(licences, WUA_ID))
        .isInstanceOf(MigrationPreconditionException.class);
  }

  @Test
  void nextBatch_whenACleardownLeavesNoMigratedSchedule_deletesEveryEarlierRun() {
    var migration = completedRun(
        runService.startRun(List.of("P1"), WUA_ID), PearsScheduleMigrationQueueStatus.MIGRATED);
    var cleardown = completedRun(
        runService.startCleardown(List.of("P1"), WUA_ID), PearsScheduleMigrationQueueStatus.CLEARED);
    entityManager.clear();

    assertThat(runService.report(migration)).isEmpty();
    assertThat(statuses(migration)).isEmpty();
    assertThat(runService.report(cleardown)).hasValueSatisfying(report -> {
      assertThat(report.status()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);
      assertThat(report.licencesByStatus()).containsEntry(PearsScheduleMigrationQueueStatus.CLEARED, 1L);
    });
  }

  @Test
  void nextBatch_whenMigratedSchedulesRemainAfterACleardown_keepsEarlierRuns() {
    var migration = completedRun(
        runService.startRun(List.of("P1"), WUA_ID), PearsScheduleMigrationQueueStatus.MIGRATED);
    givenAMigratedSchedule();
    completedRun(runService.startCleardown(List.of("P2"), WUA_ID), PearsScheduleMigrationQueueStatus.SKIPPED);
    entityManager.clear();

    assertThat(runService.report(migration)).isPresent();
    assertThat(statuses(migration)).containsExactly(entry("P1", PearsScheduleMigrationQueueStatus.MIGRATED));
  }

  @Test
  void nextBatch_whenAMigrationCompletesWithNoMigratedSchedule_keepsEarlierRuns() {
    var first = completedRun(runService.startRun(List.of("P1"), WUA_ID), PearsScheduleMigrationQueueStatus.SKIPPED);
    completedRun(runService.startRun(List.of("P1"), WUA_ID), PearsScheduleMigrationQueueStatus.SKIPPED);
    entityManager.clear();

    assertThat(runService.report(first)).isPresent();
  }

  /**
   * Gives every licence in the run the outcome given, and lets the run complete.
   */
  private UUID completedRun(PearsScheduleMigrationRun run, PearsScheduleMigrationQueueStatus outcome) {
    var batch = runService.nextBatch(10).orElseThrow();
    batch.licenceReferences().forEach(licence -> giveOutcome(run.getId(), licence, outcome, null));
    assertThat(runService.nextBatch(10)).isEmpty();
    return run.getId();
  }

  private void givenAMigratedSchedule() {
    var licence = LicenceTestUtil.builder()
        .withId(9001)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9001")
        .withLicenceReference("P9001")
        .build();
    entityManager.persist(licence);
    var schedule = new LicenceSchedule();
    schedule.setLicence(licence);
    entityManager.persist(schedule);
    entityManager.flush();
    entityManager.createNativeQuery("""
            INSERT INTO lms.pears_schedule_migrated_licences (licence_schedule_id, licence_id, run_id, migrated_at)
            VALUES (:schedule, :licence, :run, now())
            """)
        .setParameter("schedule", schedule.getId())
        .setParameter("licence", licence.getId())
        .setParameter("run", UUID.randomUUID())
        .executeUpdate();
  }

  private boolean giveOutcome(
      UUID runId,
      String licenceReference,
      PearsScheduleMigrationQueueStatus result,
      String outcome
  ) {
    return runService.recordOutcome(runId, licenceReference, result, outcome, Map.of(), Instant.now());
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
        .forEach(entry -> statuses.put(entry.getId().licenceReference(), entry.getStatus()));
    return statuses;
  }
}
