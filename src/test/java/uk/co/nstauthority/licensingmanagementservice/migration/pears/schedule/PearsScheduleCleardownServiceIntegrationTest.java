package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleWriterIntegrationTest.EVENT_TYPES;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleWriterIntegrationTest.pearsSchedule;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Migrates the P2244-shaped schedule from {@link PearsScheduleWriterIntegrationTest}, adds what users and the reminder
 * jobs hang off it, and clears it down. P9245 has a schedule LMS created itself.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class PearsScheduleCleardownServiceIntegrationTest {

  private static final UUID RUN_ID = UUID.randomUUID();
  private static final int AUDIT_REVISION = 987654;
  private static final UUID SCHEDULE_COMMENT = UUID.randomUUID();

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PearsScheduleWriter pearsScheduleWriter;

  @Autowired
  private PearsScheduleMigratedLicenceRepository migratedLicenceRepository;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private PearsScheduleCleardownService cleardownService;
  private Licence licence;
  private Licence lmsLicence;
  private LicenceSchedule lmsSchedule;

  @BeforeEach
  void setUp() {
    cleardownService = new PearsScheduleCleardownService(migratedLicenceRepository, entityManager);
    licence = persistLicence(9244);
    lmsLicence = persistLicence(9245);
    lmsSchedule = new LicenceSchedule();
    lmsSchedule.setLicence(lmsLicence);
    entityManager.persist(lmsSchedule);
  }

  @Test
  void licencesToClear_isTheLicencesWithAMigratedSchedule() {
    migrate();

    assertThat(cleardownService.licencesToClear()).containsExactly("P9244");
  }

  @Test
  void clear_deletesTheMigratedScheduleAndEverythingHangingOffIt() {
    var scheduleId = migrate();
    var events = ids("SELECT id FROM lms.schedule_events WHERE licence_schedule_id = :id", scheduleId);
    var versions = ids("SELECT id FROM lms.licence_schedule_details WHERE licence_schedule_id = :id", scheduleId);
    var event = events.getFirst();
    var deletedEvent = UUID.randomUUID();
    givenWhatHangsOffTheSchedule(scheduleId, event, deletedEvent);

    var outcome = cleardownService.clear("P9244");
    entityManager.flush();

    assertThat(outcome).isEqualTo(new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.CLEARED, null));
    assertThat(entityManager.find(Licence.class, licence.getId())).isNotNull();

    var remaining = new LinkedHashMap<String, Long>();
    remaining.put("licence_schedules", count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id", scheduleId));
    remaining.put("schedule_events", count("SELECT count(*) FROM lms.schedule_events WHERE id IN (:ids)", events));
    remaining.put("licence_schedule_details",
        count("SELECT count(*) FROM lms.licence_schedule_details WHERE id IN (:ids)", versions));
    List.of("licence_schedule_terms", "licence_schedule_phases", "licence_schedule_rates", "other_schedule_events",
            "licence_schedule_expiry_dates", "work_programme_activities")
        .forEach(table -> remaining.put(table,
            count("SELECT count(*) FROM lms.%s WHERE id IN (:ids)".formatted(table), events)));
    remaining.put("event_comments",
        count("SELECT count(*) FROM lms.event_comments WHERE event_reference_id IN (:ids)", events));
    remaining.put("work_programme_activity_statuses",
        count("SELECT count(*) FROM lms.work_programme_activity_statuses WHERE event_reference_id IN (:ids)", events));
    remaining.put("pears_schedule_event_migration_links",
        count("SELECT count(*) FROM lms.pears_schedule_event_migration_links WHERE schedule_event_id IN (:ids)", events));
    remaining.put("licence_schedule_area_events",
        count("SELECT count(*) FROM lms.licence_schedule_area_events WHERE licence_schedule_detail_id IN (:ids)", versions));
    remaining.put("licence_start_dates",
        count("SELECT count(*) FROM lms.licence_start_dates WHERE licence_schedule_detail_id IN (:ids)", versions));
    remaining.put("pears_schedule_migrated_licences",
        count("SELECT count(*) FROM lms.pears_schedule_migrated_licences WHERE licence_schedule_id = :id", scheduleId));
    remaining.put("licence_reminders",
        count("SELECT count(*) FROM lms.licence_reminders WHERE licence_id = :id", licence.getId()));
    remaining.put("licence_reminder_missing_contacts",
        count("SELECT count(*) FROM lms.licence_reminder_missing_contacts WHERE licence_id = :id", licence.getId()));
    remaining.put("licence_event_cache",
        count("SELECT count(*) FROM lms.licence_event_cache WHERE licence_id = :id", licence.getId()));
    remaining.put("licence_reminders_aud",
        count("SELECT count(*) FROM lms.licence_reminders_aud WHERE licence_id = :id", licence.getId()));
    remaining.put("schedule_events_aud",
        count("SELECT count(*) FROM lms.schedule_events_aud WHERE licence_schedule_id = :id", scheduleId));
    remaining.put("licence_schedule_rates_aud",
        count("SELECT count(*) FROM lms.licence_schedule_rates_aud WHERE id = :id", deletedEvent));
    remaining.put("licence_schedule_details_aud",
        count("SELECT count(*) FROM lms.licence_schedule_details_aud WHERE licence_schedule_id = :id", scheduleId));
    remaining.put("licence_schedules_aud",
        count("SELECT count(*) FROM lms.licence_schedules_aud WHERE id = :id", scheduleId));
    remaining.put("schedule_comments",
        count("SELECT count(*) FROM lms.schedule_comments WHERE id = :id", SCHEDULE_COMMENT));
    remaining.put("schedule_comments_aud",
        count("SELECT count(*) FROM lms.schedule_comments_aud WHERE id = :id", SCHEDULE_COMMENT));
    assertThat(remaining).allSatisfy((table, rows) -> assertThat(rows).as(table).isZero());
  }

  @Test
  void clear_leavesOtherLicencesAndSchedulesLmsCreatedAlone() {
    migrate();
    givenAnotherLicencesRows();

    cleardownService.clear("P9244");
    entityManager.flush();

    assertThat(count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id", lmsSchedule.getId())).isOne();
    assertThat(count("SELECT count(*) FROM lms.licence_event_cache WHERE licence_id = :id", lmsLicence.getId()))
        .isOne();
    assertThat(count("SELECT count(*) FROM lms.licence_reminder_missing_contacts WHERE licence_id = :id",
        lmsLicence.getId())).isOne();
    assertThat(count("SELECT count(*) FROM lms.licence_reminders WHERE licence_id = :id", lmsLicence.getId()))
        .isOne();
  }

  @Test
  void clear_whenTheLicenceHasNoMigratedSchedule_skipsIt() {
    var outcome = cleardownService.clear("P9245");

    assertThat(outcome).isEqualTo(new PearsScheduleCleardownOutcome(
        PearsScheduleCleardownOutcome.Result.SKIPPED, PearsScheduleCleardownService.NOT_MIGRATED));
    assertThat(count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id", lmsSchedule.getId())).isOne();
  }

  @Test
  void clear_whileTheReminderJobHoldsItsLock_putsItOffAndChangesNothing() {
    var scheduleId = migrate();
    givenTheReminderJobLockIsHeldFor("1 hour");

    var outcome = cleardownService.clear("P9244");
    entityManager.flush();

    assertThat(outcome).isEqualTo(new PearsScheduleCleardownOutcome(
        PearsScheduleCleardownOutcome.Result.POSTPONED, PearsScheduleCleardownService.REMINDER_JOB_RUNNING));
    assertThat(count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id", scheduleId)).isOne();
  }

  @Test
  void clear_onceTheReminderJobHasLetGoOfItsLock_clears() {
    var scheduleId = migrate();
    givenTheReminderJobLockIsHeldFor("-1 minute");

    var outcome = cleardownService.clear("P9244");
    entityManager.flush();

    assertThat(outcome.result()).isEqualTo(PearsScheduleCleardownOutcome.Result.CLEARED);
    assertThat(count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id", scheduleId)).isZero();
  }

  @Test
  void clear_beforeTheReminderJobHasEverRun_addsItsLockLetGoOf() {
    var scheduleId = migrate();

    var outcome = cleardownService.clear("P9244");
    entityManager.flush();

    assertThat(outcome.result()).isEqualTo(PearsScheduleCleardownOutcome.Result.CLEARED);
    assertThat(count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id", scheduleId)).isZero();
    // So the job's first run updates the row, and waits for a cleardown holding it, rather than inserting it
    assertThat(count("""
            SELECT count(*) FROM lms.shedlock
            WHERE name = :id AND lock_until <= timezone('utc', CURRENT_TIMESTAMP)""",
        PearsScheduleCleardownService.REMINDER_JOB_LOCK)).isOne();
  }

  @Test
  void clear_thenMigratingAgain_writesTheSameSchedule() {
    var firstSchedule = migrate();
    var firstShape = shape(firstSchedule);

    cleardownService.clear("P9244");
    entityManager.flush();
    entityManager.clear();
    licence = entityManager.find(Licence.class, licence.getId());
    var secondSchedule = migrate();

    assertThat(secondSchedule).isNotEqualTo(firstSchedule);
    assertThat(shape(secondSchedule)).isEqualTo(firstShape).containsEntry("licence_schedule_details", 2L);
  }

  private UUID migrate() {
    var pearsSchedule = pearsSchedule();
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);
    var licenceSchedule = pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, RUN_ID).orElseThrow();
    entityManager.flush();
    return licenceSchedule.getId();
  }

  /**
   * How many of each row the schedule has, which a second migration of the same PEARS schedule matches.
   */
  private Map<String, Long> shape(UUID scheduleId) {
    var events = "(SELECT id FROM lms.schedule_events WHERE licence_schedule_id = :id)";
    var versions = "(SELECT id FROM lms.licence_schedule_details WHERE licence_schedule_id = :id)";
    var shape = new LinkedHashMap<String, Long>();
    shape.put("licence_schedule_details", count("SELECT count(*) FROM lms.licence_schedule_details "
        + "WHERE licence_schedule_id = :id", scheduleId));
    shape.put("schedule_events",
        count("SELECT count(*) FROM lms.schedule_events WHERE licence_schedule_id = :id", scheduleId));
    List.of("licence_schedule_terms", "licence_schedule_rates", "other_schedule_events",
            "licence_schedule_expiry_dates")
        .forEach(table -> shape.put(table,
            count("SELECT count(*) FROM lms.%s WHERE id IN %s".formatted(table, events), scheduleId)));
    shape.put("event_comments",
        count("SELECT count(*) FROM lms.event_comments WHERE event_reference_id IN " + events, scheduleId));
    shape.put("pears_schedule_event_migration_links", count(
        "SELECT count(*) FROM lms.pears_schedule_event_migration_links WHERE schedule_event_id IN " + events,
        scheduleId));
    shape.put("licence_schedule_area_events", count(
        "SELECT count(*) FROM lms.licence_schedule_area_events WHERE licence_schedule_detail_id IN " + versions,
        scheduleId));
    return shape;
  }

  /**
   * Reminders and missing-contact reports, about one of its events and the licence's expiry, which has no event; the
   * event tracker's copy and a status on the event; and the audit history of an event a user has since deleted.
   */
  private void givenWhatHangsOffTheSchedule(UUID scheduleId, UUID event, UUID deletedEvent) {
    execute("""
        INSERT INTO lms.licence_reminders (id, schedule_event_id, original_event_id, licence_id,
          responsible_organisation_id, deadline_date, notice_period, notification_batch_reference, queued_at,
          reminder_type)
        VALUES (gen_random_uuid(), :event, :event, :licence, 1, current_date, 'SIX_MONTHS', gen_random_uuid(),
          now(), 'TERM_OR_PHASE_END'),
          (gen_random_uuid(), NULL, NULL, :licence, 1, current_date, 'TWELVE_MONTHS', gen_random_uuid(),
          now(), 'LICENCE_EXPIRY')
        """, Map.of("event", event, "licence", licence.getId()));
    execute("""
        INSERT INTO lms.licence_reminder_missing_contacts (id, licence_id, responsible_organisation_id,
          original_event_id, reminder_type, deadline_date, reported_at)
        VALUES (gen_random_uuid(), :licence, 1, :event, 'TERM_OR_PHASE_END', current_date, now()),
          (gen_random_uuid(), :licence, 1, NULL, 'LICENCE_EXPIRY', current_date, now())
        """, Map.of("event", event, "licence", licence.getId()));
    execute("""
        INSERT INTO lms.licence_event_cache (id, licence_id, licence_reference, original_event_id)
        VALUES (gen_random_uuid(), :licence, 'P9244', :event)
        """, Map.of("event", event, "licence", licence.getId()));
    execute("""
        INSERT INTO lms.work_programme_activity_statuses (id, event_reference_id, status)
        VALUES (gen_random_uuid(), :event, 'COMPLETE')
        """, Map.of("event", event));
    execute("INSERT INTO lms.audit_revisions (rev, created_date_time) VALUES (:rev, now()), (:rev + 1, now())",
        Map.of("rev", AUDIT_REVISION));
    execute("""
        INSERT INTO lms.licence_schedules_aud (rev, revtype, id, licence_id) VALUES (:rev, 0, :schedule, :licence)
        """, Map.of("rev", AUDIT_REVISION, "schedule", scheduleId, "licence", licence.getId()));
    execute("""
        INSERT INTO lms.licence_reminders_aud (rev, revtype, id, licence_id, reminder_type)
        VALUES (:rev, 0, gen_random_uuid(), :licence, 'LICENCE_EXPIRY')
        """, Map.of("rev", AUDIT_REVISION, "licence", licence.getId()));
    execute("""
        INSERT INTO lms.licence_schedule_details_aud (rev, revtype, id, licence_schedule_id)
        VALUES (:rev, 0, gen_random_uuid(), :schedule)
        """, Map.of("rev", AUDIT_REVISION, "schedule", scheduleId));
    execute("""
        INSERT INTO lms.schedule_events_aud (rev, revtype, id, licence_schedule_id, event_type)
        VALUES (:rev, 0, :event, :schedule, 'RATE'), (:rev + 1, 2, :event, :schedule, 'RATE')
        """, Map.of("rev", AUDIT_REVISION, "event", deletedEvent, "schedule", scheduleId));
    execute("""
        INSERT INTO lms.licence_schedule_rates_aud (rev, revtype, id) VALUES (:rev, 0, :event), (:rev + 1, 2, :event)
        """, Map.of("rev", AUDIT_REVISION, "event", deletedEvent));
    execute("""
        INSERT INTO lms.schedule_comments (id, licence_schedule_id, comment, timestamp)
        VALUES (:comment, :schedule, 'Migrated general note', now())
        """, Map.of("comment", SCHEDULE_COMMENT, "schedule", scheduleId));
    execute("""
        INSERT INTO lms.schedule_comments_aud (rev, revtype, id, licence_schedule_id) VALUES (:rev, 0, :comment, :schedule)
        """, Map.of("rev", AUDIT_REVISION, "comment", SCHEDULE_COMMENT, "schedule", scheduleId));
  }

  private void givenAnotherLicencesRows() {
    execute("""
        INSERT INTO lms.licence_event_cache (id, licence_id, licence_reference) VALUES (gen_random_uuid(), :licence, 'P9245')
        """, Map.of("licence", lmsLicence.getId()));
    execute("""
        INSERT INTO lms.licence_reminder_missing_contacts (id, licence_id, responsible_organisation_id,
          original_event_id, reminder_type, deadline_date, reported_at)
        VALUES (gen_random_uuid(), :licence, 1, NULL, 'LICENCE_EXPIRY', current_date, now())
        """, Map.of("licence", lmsLicence.getId()));
    execute("""
        INSERT INTO lms.licence_reminders (id, licence_id, responsible_organisation_id, deadline_date, notice_period,
          notification_batch_reference, queued_at, reminder_type)
        VALUES (gen_random_uuid(), :licence, 1, current_date, 'TWELVE_MONTHS', gen_random_uuid(), now(),
          'LICENCE_EXPIRY')
        """, Map.of("licence", lmsLicence.getId()));
  }

  private Licence persistLicence(int id) {
    var newLicence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber(String.valueOf(id))
        .withLicenceReference("P" + id)
        .build();
    entityManager.persist(newLicence);
    return newLicence;
  }

  /**
   * As ShedLock holds it, in UTC database time. A lock held for a negative interval was let go of.
   */
  private void givenTheReminderJobLockIsHeldFor(String interval) {
    execute("""
            INSERT INTO lms.shedlock (name, lock_until, locked_at, locked_by)
            VALUES (:name, timezone('utc', CURRENT_TIMESTAMP) + CAST(:interval AS INTERVAL),
                timezone('utc', CURRENT_TIMESTAMP), 'test')
            ON CONFLICT (name) DO UPDATE SET lock_until = EXCLUDED.lock_until""",
        Map.of("name", PearsScheduleCleardownService.REMINDER_JOB_LOCK, "interval", interval));
  }

  private void execute(String sql, Map<String, Object> parameters) {
    var query = entityManager.createNativeQuery(sql);
    parameters.forEach(query::setParameter);
    query.executeUpdate();
  }

  @SuppressWarnings("unchecked")
  private List<UUID> ids(String sql, Object id) {
    return entityManager.createNativeQuery(sql, UUID.class).setParameter("id", id).getResultList();
  }

  private long count(String sql, Object parameter) {
    var name = parameter instanceof List<?> ? "ids" : "id";
    return ((Number) entityManager.createNativeQuery(sql).setParameter(name, parameter).getSingleResult()).longValue();
  }
}
