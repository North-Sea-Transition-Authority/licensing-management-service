package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.P9244;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.activity;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.note;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.programme;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleComment;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MigratedScheduleFixture;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownOutcome;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Sql(
    scripts = {
        "classpath:migration/create-pears-schedule-migration-tables.sql",
        "classpath:migration/create-pears-work-programme-migration-tables.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Import(MigratedScheduleFixture.class)
@Transactional
@IntegrationTest
class PearsWorkProgrammeCleardownServiceIntegrationTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private MigratedScheduleFixture migratedScheduleFixture;

  @Autowired
  private MigratedScheduleReader migratedScheduleReader;

  @Autowired
  private PearsWorkProgrammeWriter pearsWorkProgrammeWriter;

  @Autowired
  private PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private PearsWorkProgrammeCleardownService cleardownService;

  private Licence licence;

  private LicenceSchedule licenceSchedule;

  @BeforeEach
  void setUp() {
    cleardownService = new PearsWorkProgrammeCleardownService(
        migratedLicenceRepository, migratedScheduleFixture.cleardownService(), entityManager);

    licence = LicenceTestUtil.builder()
        .withId(9244)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9244")
        .withLicenceReference("P9244")
        .build();
    entityManager.persist(licence);
    licenceSchedule = migratedScheduleFixture.migrate(licence);
    entityManager.flush();
  }

  @Test
  void licencesToClear_isTheLicencesWithAMigratedWorkProgramme() {
    assertThat(cleardownService.licencesToClear()).doesNotContain("P9244");
    migrate();

    assertThat(cleardownService.licencesToClear()).contains("P9244");
  }

  @Test
  void clear_deletesTheMigratedWorkProgrammeAndLeavesTheScheduleAndItsOtherComments() {
    migrate();
    var addedInLms = new ScheduleComment();
    addedInLms.setLicenceSchedule(licenceSchedule);
    addedInLms.setComment("Added in LMS");
    addedInLms.setTimestamp(Instant.now());
    entityManager.persist(addedInLms);
    entityManager.flush();

    var outcome = cleardownService.clear("P9244");
    entityManager.flush();
    entityManager.clear();

    assertThat(outcome).isEqualTo(new PearsScheduleCleardownOutcome(PearsScheduleCleardownOutcome.Result.CLEARED, null));
    assertThat(remaining()).allSatisfy((table, rows) -> assertThat(rows).as(table).isZero());
    assertThat(scheduleComments()).containsExactly("Added in LMS");
    assertThat(migratedScheduleReader.read(licence)).isPresent();
  }

  @Test
  void clear_thenMigrateAgain_writesTheSameWorkProgramme() {
    migrate();
    var before = activityDescriptions();
    cleardownService.clear("P9244");
    entityManager.flush();
    // As a run does, which migrates in a transaction of its own after the cleardown's
    entityManager.clear();

    migrate();

    assertThat(activityDescriptions()).isEqualTo(before).hasSize(2);
    assertThat(scheduleComments()).containsExactly("General note (PEARS note by John Smith)");
  }

  @Test
  void clear_whenTheWorkProgrammeWasNotMigrated_skips() {
    assertThat(cleardownService.clear("P9244")).isEqualTo(new PearsScheduleCleardownOutcome(
        PearsScheduleCleardownOutcome.Result.SKIPPED, PearsWorkProgrammeCleardownService.NOT_MIGRATED));
  }

  @Test
  void scheduleCleardown_onceTheWorkProgrammeIsMigrated_stillClearsEverything() {
    migrate();

    migratedScheduleFixture.clear(licence);
    entityManager.flush();

    assertThat(remaining()).allSatisfy((table, rows) -> assertThat(rows).as(table).isZero());
    assertThat(count("SELECT count(*) FROM lms.licence_schedules WHERE id = :id")).isZero();
    assertThat(count("SELECT count(*) FROM lms.schedule_comments WHERE licence_schedule_id = :id")).isZero();
  }

  private void migrate() {
    var schedule = migratedScheduleReader.read(licence).orElseThrow().schedule();
    var mapped = PearsWorkProgrammeMapper.map(P9244, List.of(programme(
        List.of(
            activity("1", "DRILL_OR_DROP_WELL", "Drill or drop well to 3,000m", "DROPPED"),
            activity("2", "OTHER", "Geological studies", "IN_PROGRESS")
        ),
        List.of(
            note("2", "IN_PROGRESS", LocalDateTime.of(2016, Month.JULY, 1, 9, 30), "Jane Smith", "Studies started"),
            note(null, null, LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0), "John Smith", "General note")
        )
    )), schedule);
    assertThat(pearsWorkProgrammeWriter.write(licence, mapped, UUID.randomUUID())).isTrue();
    entityManager.flush();
  }

  private Map<String, Long> remaining() {
    var activities = "SELECT a.id FROM lms.work_programme_activities a "
        + "JOIN lms.schedule_events e ON e.id = a.id WHERE e.licence_schedule_id = :id";
    var remaining = new LinkedHashMap<String, Long>();
    remaining.put("work_programme_activities", count("SELECT count(*) FROM (%s) a".formatted(activities)));
    remaining.put("work_programme_activity_statuses", count(
        "SELECT count(*) FROM lms.work_programme_activity_statuses WHERE event_reference_id IN (%s)"
            .formatted(activities)));
    remaining.put("event_comments", count(
        "SELECT count(*) FROM lms.event_comments WHERE event_reference_id IN (%s)".formatted(activities)));
    remaining.put("licence_reminders", count(
        "SELECT count(*) FROM lms.licence_reminders WHERE schedule_event_id IN (%s)".formatted(activities)));
    remaining.put("pears_work_programme_activity_migration_links", count(
        "SELECT count(*) FROM lms.pears_work_programme_activity_migration_links"));
    remaining.put("pears_work_programme_schedule_comment_links", count(
        "SELECT count(*) FROM lms.pears_work_programme_schedule_comment_links"));
    remaining.put("pears_work_programme_migrated_licences", count(
        "SELECT count(*) FROM lms.pears_work_programme_migrated_licences WHERE licence_schedule_id = :id"));
    return remaining;
  }

  private List<String> activityDescriptions() {
    return entityManager.createNativeQuery("""
            SELECT a.description FROM lms.work_programme_activities a
            JOIN lms.schedule_events e ON e.id = a.id
            WHERE e.licence_schedule_id = :id
            ORDER BY a.description""")
        .setParameter("id", licenceSchedule.getId())
        .getResultList().stream()
        .map(String.class::cast)
        .toList();
  }

  private List<String> scheduleComments() {
    return entityManager.createNativeQuery(
            "SELECT comment FROM lms.schedule_comments WHERE licence_schedule_id = :id ORDER BY comment")
        .setParameter("id", licenceSchedule.getId())
        .getResultList().stream()
        .map(String.class::cast)
        .toList();
  }

  private long count(String sql) {
    var query = entityManager.createNativeQuery(sql);
    if (sql.contains(":id")) {
      query.setParameter("id", licenceSchedule.getId());
    }
    return ((Number) query.getSingleResult()).longValue();
  }
}
