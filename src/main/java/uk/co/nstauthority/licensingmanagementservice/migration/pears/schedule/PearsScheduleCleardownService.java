package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Deletes the schedules the PEARS schedule migration created, so the migration can be run again (plan, design
 * section 4a). Only the schedule goes, never the licence, and schedules LMS created itself are never touched.
 * Environments are reset before a migration, so no application depends on a migrated schedule; if one did, its
 * foreign key would stop the delete and the licence would fail.
 *
 * <p>Bulk deletes, so rows that no longer load are cleared all the same, and every delete is limited to the
 * schedule's own rows. The audit history goes too, found through the audit rows as well as the live ones, so events a
 * user has since deleted are cleared with the rest.</p>
 */
@Service
@Profile("migration")
public class PearsScheduleCleardownService {

  static final String NOT_MIGRATED = "Has no schedule from the PEARS migration";
  static final String REMINDER_JOB_RUNNING = "The licence reminder job is running, so cleared after it finishes";

  // EventReminderJob's ShedLock name
  static final String REMINDER_JOB_LOCK = "licenceReminders";

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsScheduleCleardownService.class);

  // The parameter names in the SQL below
  private static final String SCHEDULE = "schedule";
  private static final String LICENCE = "licence";

  private static final String EVENTS = """
      SELECT id FROM lms.schedule_events WHERE licence_schedule_id = :schedule
      UNION SELECT id FROM lms.schedule_events_aud WHERE licence_schedule_id = :schedule""";

  private static final String VERSIONS = """
      SELECT id FROM lms.licence_schedule_details WHERE licence_schedule_id = :schedule
      UNION SELECT id FROM lms.licence_schedule_details_aud WHERE licence_schedule_id = :schedule""";

  // Expiry reminders belong to the licence rather than an event (the event is optional, the original event never set),
  // and the expiry comes only from the schedule
  private static final String LICENCE_EXPIRY =
      "(licence_id = :licence AND reminder_type = 'LICENCE_EXPIRY' AND original_event_id IS NULL)";

  private static final String REMINDERS = "schedule_event_id IN (%s) OR %s".formatted(EVENTS, LICENCE_EXPIRY);

  private static final String MISSING_CONTACTS = "original_event_id IN (%s) OR %s".formatted(EVENTS, LICENCE_EXPIRY);

  // Children before parents
  private static final List<String> EVENT_TABLES = List.of(
      "licence_schedule_rates",
      "other_schedule_events",
      "work_programme_activities",
      "licence_schedule_expiry_dates",
      "licence_schedule_phases",
      "licence_schedule_terms"
  );

  private final PearsScheduleMigratedLicenceRepository migratedLicenceRepository;
  private final EntityManager entityManager;

  PearsScheduleCleardownService(
      PearsScheduleMigratedLicenceRepository migratedLicenceRepository,
      EntityManager entityManager
  ) {
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.entityManager = entityManager;
  }

  /**
   * The licences with a schedule from the migration, to clear one by one.
   */
  public List<String> licencesToClear() {
    return migratedLicenceRepository.findAllLicenceReferences();
  }

  /**
   * Clears one licence's migrated schedule, or puts it off while the reminder job is running.
   */
  @Transactional
  public PearsScheduleCleardownOutcome clear(String licenceReference) {
    if (reminderJobRunning()) {
      return PearsScheduleCleardownOutcome.postponed(REMINDER_JOB_RUNNING);
    }

    var foundMigratedLicence = migratedLicenceRepository.findByLicenceReference(licenceReference);
    if (foundMigratedLicence.isEmpty()) {
      return PearsScheduleCleardownOutcome.skipped(NOT_MIGRATED);
    }

    var migratedLicence = foundMigratedLicence.get();
    var scheduleId = migratedLicence.getLicenceScheduleId();
    lockEvents(scheduleId);
    deleteAuditHistory(scheduleId, migratedLicence.getLicenceId());
    deleteSchedule(scheduleId, migratedLicence.getLicenceId());

    LOGGER.info("Cleared the migrated schedule {} of {}", scheduleId, licenceReference);
    return PearsScheduleCleardownOutcome.cleared();
  }

  /**
   * An expiry reminder belongs to the licence alone, so locking the events can't hold back one the reminder job is about
   * to write from deadlines it read before the cleardown. Locking the job's ShedLock row does: ShedLock takes the lock by
   * updating that row, so a job starting now can't take it, or read any deadline, until the cleardown commits. While
   * the job holds the lock it may already have read the deadlines, so the cleardown waits for it.
   *
   * <p>ShedLock only adds the row the first time the job runs, so it is added here first if missing, already let go of.
   * The job's first run then updates it, and waits for the cleardown like any other.
   */
  private boolean reminderJobRunning() {
    entityManager.createNativeQuery("""
            INSERT INTO lms.shedlock (name, lock_until, locked_at, locked_by)
            VALUES (:name, timezone('utc', CURRENT_TIMESTAMP), timezone('utc', CURRENT_TIMESTAMP), :lockedBy)
            ON CONFLICT (name) DO NOTHING""")
        .setParameter("name", REMINDER_JOB_LOCK)
        .setParameter("lockedBy", "pears-schedule-cleardown")
        .executeUpdate();
    // ShedLock is set to use database time, which it holds in UTC
    var lockedNow = entityManager.createNativeQuery("""
            SELECT lock_until > timezone('utc', CURRENT_TIMESTAMP)
            FROM lms.shedlock
            WHERE name = :name
            FOR UPDATE""")
        .setParameter("name", REMINDER_JOB_LOCK)
        .getResultList();
    return !lockedNow.isEmpty() && Boolean.TRUE.equals(lockedNow.getFirst());
  }

  /**
   * The reminder job may be writing a reminder about one of the events at the same time. Locking them first waits for
   * one already being written, so it is deleted with the rest, and makes one written later wait and then fail its
   * foreign key, rather than the cleardown failing on it.
   */
  private void lockEvents(UUID scheduleId) {
    entityManager.createNativeQuery("SELECT id FROM lms.schedule_events WHERE licence_schedule_id = :schedule FOR UPDATE")
        .setParameter(SCHEDULE, scheduleId)
        .getResultList();
  }

  /**
   * Done first, while the live rows it is partly found through are there. The audit rows of the events and versions
   * themselves go last, in {@link #deleteSchedule}, as the rest are found through them too.
   */
  private void deleteAuditHistory(UUID scheduleId, int licenceId) {
    delete("DELETE FROM lms.licence_reminders_aud WHERE " + REMINDERS, scheduleId, licenceId);
    delete("DELETE FROM lms.licence_reminder_missing_contacts_aud WHERE " + MISSING_CONTACTS, scheduleId, licenceId);
    delete("DELETE FROM lms.licence_event_cache_aud WHERE original_event_id IN (%s)"
        .formatted(EVENTS), scheduleId);
    delete("DELETE FROM lms.event_comments_aud WHERE event_reference_id IN (%s)".formatted(EVENTS), scheduleId);
    delete("DELETE FROM lms.work_programme_activity_statuses_aud WHERE event_reference_id IN (%s)".formatted(EVENTS),
        scheduleId);
    EVENT_TABLES.forEach(table ->
        delete("DELETE FROM lms.%s_aud WHERE id IN (%s)".formatted(table, EVENTS), scheduleId));
    delete("DELETE FROM lms.licence_schedule_area_events_aud WHERE licence_schedule_detail_id IN (%s)"
        .formatted(VERSIONS), scheduleId);
    delete("DELETE FROM lms.licence_start_dates_aud WHERE licence_schedule_detail_id IN (%s)".formatted(VERSIONS),
        scheduleId);
    delete("DELETE FROM lms.schedule_comments_aud WHERE licence_schedule_id = :schedule", scheduleId);
  }

  private void deleteSchedule(UUID scheduleId, int licenceId) {
    delete("DELETE FROM lms.licence_reminders WHERE " + REMINDERS, scheduleId, licenceId);
    delete("DELETE FROM lms.licence_reminder_missing_contacts WHERE " + MISSING_CONTACTS, scheduleId, licenceId);
    delete("DELETE FROM lms.licence_event_cache WHERE original_event_id IN (%s)"
        .formatted(EVENTS), scheduleId);
    delete("DELETE FROM lms.event_comments WHERE event_reference_id IN (%s)".formatted(EVENTS), scheduleId);
    delete("DELETE FROM lms.work_programme_activity_statuses WHERE event_reference_id IN (%s)".formatted(EVENTS),
        scheduleId);
    delete("DELETE FROM lms.pears_schedule_event_migration_links WHERE schedule_event_id IN (%s)".formatted(EVENTS),
        scheduleId);
    EVENT_TABLES.forEach(table -> delete("DELETE FROM lms.%s WHERE id IN (%s)".formatted(table, EVENTS), scheduleId));
    delete("DELETE FROM lms.schedule_events WHERE licence_schedule_id = :schedule", scheduleId);

    delete("DELETE FROM lms.licence_schedule_area_events WHERE licence_schedule_detail_id IN (%s)".formatted(VERSIONS),
        scheduleId);
    delete("DELETE FROM lms.licence_start_dates WHERE licence_schedule_detail_id IN (%s)".formatted(VERSIONS),
        scheduleId);
    delete("DELETE FROM lms.licence_schedule_details WHERE licence_schedule_id = :schedule", scheduleId);
    delete("DELETE FROM lms.pears_schedule_migrated_licences WHERE licence_schedule_id = :schedule", scheduleId);
    delete("DELETE FROM lms.schedule_comments WHERE licence_schedule_id = :schedule", scheduleId);
    delete("DELETE FROM lms.licence_schedules WHERE id = :schedule", scheduleId);

    delete("DELETE FROM lms.schedule_events_aud WHERE licence_schedule_id = :schedule", scheduleId);
    delete("DELETE FROM lms.licence_schedule_details_aud WHERE licence_schedule_id = :schedule", scheduleId);
    delete("DELETE FROM lms.licence_schedules_aud WHERE id = :schedule", scheduleId);
  }

  private void delete(String sql, UUID scheduleId) {
    entityManager.createNativeQuery(sql)
        .setParameter(SCHEDULE, scheduleId)
        .executeUpdate();
  }

  private void delete(String sql, UUID scheduleId, int licenceId) {
    entityManager.createNativeQuery(sql)
        .setParameter(SCHEDULE, scheduleId)
        .setParameter(LICENCE, licenceId)
        .executeUpdate();
  }
}
