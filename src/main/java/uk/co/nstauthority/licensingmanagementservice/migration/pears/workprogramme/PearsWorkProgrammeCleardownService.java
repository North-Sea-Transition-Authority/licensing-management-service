package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;

/**
 * Deletes the work programmes the PEARS work programme migration wrote, so it can be run again (plan, design section
 * 4a). The schedule stays, ready for the work programme to be migrated onto it again, and the schedule comments users
 * added in LMS stay with it.
 *
 * <p>Bulk deletes, limited to the migrated activities and every later copy of them (found by {@code originalEventId},
 * which for a migrated activity is its own id), with their audit rows.</p>
 */
@Service
@Profile("migration")
public class PearsWorkProgrammeCleardownService {

  static final String NOT_MIGRATED = "Has no work programme from the PEARS migration";
  static final String REMINDER_JOB_RUNNING = "The licence reminder job is running, so cleared after it finishes";

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsWorkProgrammeCleardownService.class);

  // The parameter names in the SQL below
  private static final String SCHEDULE = "schedule";
  private static final String EVENTS = "events";
  private static final String COMMENTS = "comments";

  // The migrated activities on the schedule: the ids the link rows name, each its own originalEventId
  private static final String MIGRATED_ACTIVITIES = """
      SELECT l.schedule_event_id
      FROM lms.pears_work_programme_activity_migration_links l
      JOIN lms.schedule_events e ON e.id = l.schedule_event_id
      WHERE e.licence_schedule_id = :schedule""";

  private static final String LOCK_ACTIVITY_EVENTS =
      "SELECT id FROM lms.schedule_events WHERE original_event_id IN (" + MIGRATED_ACTIVITIES + ") FOR UPDATE";

  private static final String ACTIVITY_EVENTS =
      "SELECT id FROM lms.schedule_events WHERE original_event_id IN (" + MIGRATED_ACTIVITIES + ") " +
          "UNION SELECT id FROM lms.schedule_events_aud WHERE original_event_id IN (" + MIGRATED_ACTIVITIES + ") " +
          "UNION " + MIGRATED_ACTIVITIES;

  private final PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository;
  private final PearsScheduleCleardownService pearsScheduleCleardownService;
  private final EntityManager entityManager;

  PearsWorkProgrammeCleardownService(
      PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository,
      PearsScheduleCleardownService pearsScheduleCleardownService,
      EntityManager entityManager
  ) {
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.pearsScheduleCleardownService = pearsScheduleCleardownService;
    this.entityManager = entityManager;
  }

  /**
   * The licences with a work programme from the migration, to clear one by one.
   */
  public List<String> licencesToClear() {
    return migratedLicenceRepository.findAllLicenceReferences();
  }

  /**
   * Clears one licence's migrated work programme, or puts it off while the reminder job is running.
   */
  @Transactional
  public PearsScheduleCleardownOutcome clear(String licenceReference) {
    if (pearsScheduleCleardownService.reminderJobRunning()) {
      return new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.POSTPONED, REMINDER_JOB_RUNNING);
    }

    var foundMigratedLicence = migratedLicenceRepository.findByLicenceReference(licenceReference);
    if (foundMigratedLicence.isEmpty()) {
      return new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.SKIPPED, NOT_MIGRATED);
    }

    var migratedLicence = foundMigratedLicence.get();
    var scheduleId = migratedLicence.getLicenceScheduleId();
    var events = activityEvents(scheduleId);
    var comments = scheduleComments(scheduleId);

    if (!events.isEmpty()) {
      deleteActivities(events);
    }
    if (!comments.isEmpty()) {
      delete("DELETE FROM lms.schedule_comments_aud WHERE id IN (:comments)", COMMENTS, comments);
      delete("DELETE FROM lms.pears_work_programme_schedule_comment_links WHERE schedule_comment_id IN (:comments)",
          COMMENTS, comments);
      delete("DELETE FROM lms.schedule_comments WHERE id IN (:comments)", COMMENTS, comments);
    }
    delete("DELETE FROM lms.pears_work_programme_migrated_licences WHERE licence_schedule_id = :schedule",
        SCHEDULE, scheduleId);

    LOGGER.info("Cleared the migrated work programme of {}: {} activity rows, {} schedule comments",
        licenceReference, events.size(), comments.size());
    return new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.CLEARED, null);
  }

  /**
   * Every row of every migrated activity on the schedule, live or audited, locked so nothing changes them meanwhile.
   */
  private List<UUID> activityEvents(UUID scheduleId) {
    entityManager.createNativeQuery(LOCK_ACTIVITY_EVENTS)
        .setParameter(SCHEDULE, scheduleId)
        .getResultList();
    return uuids(entityManager.createNativeQuery(ACTIVITY_EVENTS)
        .setParameter(SCHEDULE, scheduleId)
        .getResultList());
  }

  private List<UUID> scheduleComments(UUID scheduleId) {
    return uuids(entityManager.createNativeQuery("""
            SELECT l.schedule_comment_id
            FROM lms.pears_work_programme_schedule_comment_links l
            JOIN lms.schedule_comments c ON c.id = l.schedule_comment_id
            WHERE c.licence_schedule_id = :schedule""")
        .setParameter(SCHEDULE, scheduleId)
        .getResultList());
  }

  /**
   * Audit rows first, then children before parents.
   */
  private void deleteActivities(List<UUID> events) {
    delete("DELETE FROM lms.licence_reminders_aud WHERE schedule_event_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.licence_reminder_missing_contacts_aud WHERE original_event_id IN (:events)", EVENTS,
        events);
    delete("DELETE FROM lms.licence_event_cache_aud WHERE original_event_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.event_comments_aud WHERE event_reference_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.work_programme_activity_statuses_aud WHERE event_reference_id IN (:events)", EVENTS,
        events);
    delete("DELETE FROM lms.work_programme_activities_aud WHERE id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.schedule_events_aud WHERE id IN (:events)", EVENTS, events);

    delete("DELETE FROM lms.licence_reminders WHERE schedule_event_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.licence_reminder_missing_contacts WHERE original_event_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.licence_event_cache WHERE original_event_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.event_comments WHERE event_reference_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.work_programme_activity_statuses WHERE event_reference_id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.pears_work_programme_activity_migration_links WHERE schedule_event_id IN (:events)",
        EVENTS, events);
    delete("DELETE FROM lms.work_programme_activities WHERE id IN (:events)", EVENTS, events);
    delete("DELETE FROM lms.schedule_events WHERE id IN (:events)", EVENTS, events);
  }

  private void delete(String sql, String parameter, Object value) {
    entityManager.createNativeQuery(sql)
        .setParameter(parameter, value)
        .executeUpdate();
  }

  private static List<UUID> uuids(List<?> rows) {
    return rows.stream().map(UUID.class::cast).toList();
  }
}
