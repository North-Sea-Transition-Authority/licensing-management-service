package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Two requests on the runs at once, each in a transaction of its own. The first holds its transaction open until the
 * second is waiting on it, then commits.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@IntegrationTest
class PearsScheduleMigrationRunServiceConcurrencyIntegrationTest {

  private static final String MADE_ACTIVE_AT_THE_SAME_TIME =
      "Another run was made active at the same time. Wait for it to finish or cancel it first.";

  @Autowired
  private PearsScheduleMigrationRunService runService;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  @AfterEach
  void tearDown() {
    jdbcTemplate.update("DELETE FROM lms.pears_schedule_migration_queue");
    jdbcTemplate.update("DELETE FROM lms.pears_schedule_migration_runs");
  }

  @Test
  void startRun_whenAnotherRunIsStartedAtTheSameTime_isRefused() throws Exception {
    var outcome = whileAnotherTransactionHolds(
        () -> runService.startRun(List.of("P1"), 1L),
        () -> runService.startRun(List.of("P2"), 1L)
    );

    assertThatThrownBy(outcome::get)
        .isInstanceOf(ExecutionException.class)
        .cause()
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessage(MADE_ACTIVE_AT_THE_SAME_TIME);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lms.pears_schedule_migration_runs", Long.class))
        .isEqualTo(1L);
  }

  @Test
  void retryFailed_whenAnotherRunIsStartedAtTheSameTime_isRefused() throws Exception {
    var completed = runService.startRun(List.of("P1"), 1L);
    runService.recordOutcome(
        completed.getId(), "P1", PearsScheduleMigrationQueueStatus.FAILED, "Could not be written", Map.of(),
        Instant.now());
    runService.nextBatch(10);

    var outcome = whileAnotherTransactionHolds(
        () -> runService.startRun(List.of("P2"), 1L),
        () -> runService.retryFailed(completed.getId())
    );

    assertThatThrownBy(outcome::get)
        .isInstanceOf(ExecutionException.class)
        .cause()
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessage(MADE_ACTIVE_AT_THE_SAME_TIME);
    assertThat(runService.report(completed.getId()))
        .hasValueSatisfying(report -> {
          assertThat(report.status()).isEqualTo(PearsScheduleMigrationRunStatus.COMPLETE);
          assertThat(report.licencesByStatus()).containsEntry(PearsScheduleMigrationQueueStatus.FAILED, 1L);
        });
  }

  @Test
  void nextBatch_whenTheRunIsCancelledAtTheSameTime_leavesItCancelled() throws Exception {
    var run = runService.startRun(List.of("P1"), 1L);

    var outcome = whileAnotherTransactionHolds(
        () -> runService.cancel(run.getId()),
        () -> runService.nextBatch(10)
    );

    assertThat(outcome.get()).isEmpty();
    assertThat(runService.report(run.getId()))
        .hasValueSatisfying(report -> assertThat(report.status()).isEqualTo(PearsScheduleMigrationRunStatus.CANCELLED));
  }

  /**
   * Runs {@code first} in a transaction it holds open, then {@code second} on another thread, and lets the first
   * commit once the second is waiting on one of its locks.
   */
  private <T> Future<T> whileAnotherTransactionHolds(Runnable first, Supplier<T> second)
      throws InterruptedException {
    var firstDone = new CountDownLatch(1);
    var commitFirst = new CountDownLatch(1);
    var transactionTemplate = new TransactionTemplate(transactionManager);
    Future<T> secondTransaction;

    // Closing the executor waits for both to finish
    try (var executor = Executors.newFixedThreadPool(2)) {
      var firstTransaction = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
        first.run();
        firstDone.countDown();
        awaitQuietly(commitFirst);
      }));
      assertThat(firstDone.await(10, TimeUnit.SECONDS)).isTrue();

      secondTransaction = executor.submit(second::get);
      waitUntilWaitingOnALock(secondTransaction);
      commitFirst.countDown();

      assertThat(firstTransaction).succeedsWithin(Duration.ofSeconds(10));
    }
    return secondTransaction;
  }

  /**
   * Checks the database until a transaction is waiting on a lock, giving the second transaction a moment between
   * checks. It should be the one waiting, so it finishing first fails the test.
   */
  private void waitUntilWaitingOnALock(Future<?> secondTransaction) throws InterruptedException {
    var giveUpAt = Instant.now().plusSeconds(10);
    while (Instant.now().isBefore(giveUpAt)) {
      var waiting = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND datname = current_database()",
          Long.class);
      if (waiting != null && waiting > 0) {
        return;
      }
      try {
        secondTransaction.get(20, TimeUnit.MILLISECONDS);
        throw new AssertionError("The second transaction finished without waiting on the first");
      } catch (ExecutionException e) {
        throw new AssertionError("The second transaction failed without waiting on the first", e);
      } catch (TimeoutException e) {
        // Still running, so check again
      }
    }
    throw new AssertionError("The second transaction never waited on the first");
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
