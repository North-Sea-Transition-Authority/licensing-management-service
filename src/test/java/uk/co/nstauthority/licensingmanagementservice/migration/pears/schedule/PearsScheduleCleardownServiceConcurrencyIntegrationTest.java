package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleWriterIntegrationTest.EVENT_TYPES;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleWriterIntegrationTest.pearsSchedule;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * The reminder job writing a reminder about one of a schedule's events while the schedule is cleared down, each in a
 * transaction of its own, so the data is committed and cleared up afterwards.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@IntegrationTest
class PearsScheduleCleardownServiceConcurrencyIntegrationTest {

  private static final int LICENCE_ID = 9246;
  private static final String LICENCE_REFERENCE = "P9246";

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PearsScheduleWriter pearsScheduleWriter;

  @Autowired
  private PearsScheduleMigratedLicenceRepository migratedLicenceRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private TransactionTemplate transactionTemplate;
  private PearsScheduleCleardownService cleardownService;
  private UUID event;

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    cleardownService = new PearsScheduleCleardownService(migratedLicenceRepository, entityManager);
    var scheduleId = transactionTemplate.execute(status -> {
      var licence = LicenceTestUtil.builder()
          .withId(LICENCE_ID)
          .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
          .withLicenceNumber(String.valueOf(LICENCE_ID))
          .withLicenceReference(LICENCE_REFERENCE)
          .build();
      entityManager.persist(licence);
      var pearsSchedule = pearsSchedule();
      var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);
      return pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, UUID.randomUUID()).orElseThrow().getId();
    });
    event = jdbcTemplate.queryForObject(
        "SELECT id FROM lms.schedule_events WHERE licence_schedule_id = ? LIMIT 1", UUID.class, scheduleId);
  }

  @AfterEach
  void tearDown() {
    jdbcTemplate.update("DELETE FROM lms.licence_reminders WHERE licence_id = ?", LICENCE_ID);
    transactionTemplate.execute(status -> cleardownService.clear(LICENCE_REFERENCE));
    jdbcTemplate.update("DELETE FROM lms.licences_aud WHERE id = ?", LICENCE_ID);
    jdbcTemplate.update("DELETE FROM lms.licences WHERE id = ?", LICENCE_ID);
  }

  @Test
  void clear_whileAReminderAboutOneOfItsEventsIsBeingWritten_waitsForItAndClearsItToo() throws Exception {
    var reminderWritten = new CountDownLatch(1);
    var commitReminder = new CountDownLatch(1);
    Future<PearsScheduleCleardownOutcome> cleardown;

    try (var executor = Executors.newFixedThreadPool(2)) {
      var reminder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
        jdbcTemplate.update("""
            INSERT INTO lms.licence_reminders (id, schedule_event_id, original_event_id, licence_id,
              responsible_organisation_id, deadline_date, notice_period, notification_batch_reference, queued_at,
              reminder_type)
            VALUES (gen_random_uuid(), ?, ?, ?, 1, current_date, 'SIX_MONTHS', gen_random_uuid(), now(),
              'TERM_OR_PHASE_END')
            """, event, event, LICENCE_ID);
        reminderWritten.countDown();
        awaitQuietly(commitReminder);
      }));
      assertThat(reminderWritten.await(10, TimeUnit.SECONDS)).isTrue();

      cleardown = executor.submit(() -> transactionTemplate.execute(status -> cleardownService.clear(LICENCE_REFERENCE)));
      waitUntilWaitingOnALock(cleardown);
      commitReminder.countDown();

      assertThat(reminder).succeedsWithin(Duration.ofSeconds(10));
    }

    assertThat(cleardown.get()).isEqualTo(
        new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.CLEARED, null));
    assertThat(jdbcTemplate.queryForObject(
        "SELECT count(*) FROM lms.licence_reminders WHERE licence_id = ?", Long.class, LICENCE_ID)).isZero();
  }

  private void waitUntilWaitingOnALock(Future<?> cleardown) throws InterruptedException {
    var giveUpAt = Instant.now().plusSeconds(10);
    while (Instant.now().isBefore(giveUpAt)) {
      var waiting = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND datname = current_database()",
          Long.class);
      if (waiting != null && waiting > 0) {
        return;
      }
      try {
        cleardown.get(20, TimeUnit.MILLISECONDS);
        throw new AssertionError("The cleardown finished without waiting on the reminder");
      } catch (ExecutionException e) {
        throw new AssertionError("The cleardown failed without waiting on the reminder", e);
      } catch (TimeoutException e) {
        // Still running, so check again
      }
    }
    throw new AssertionError("The cleardown never waited on the reminder");
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
