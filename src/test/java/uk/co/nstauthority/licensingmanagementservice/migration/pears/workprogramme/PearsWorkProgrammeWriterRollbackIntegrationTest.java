package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.P9244;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.activity;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.programme;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MigratedScheduleFixture;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Not transactional itself, so the writer's own transaction commits or rolls back as it would in a run.
 */
@Sql(
    scripts = {
        "classpath:migration/create-pears-schedule-migration-tables.sql",
        "classpath:migration/create-pears-work-programme-migration-tables.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Import(MigratedScheduleFixture.class)
@IntegrationTest
class PearsWorkProgrammeWriterRollbackIntegrationTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionOperations transactionOperations;

  @Autowired
  private MigratedScheduleFixture migratedScheduleFixture;

  @Autowired
  private MigratedScheduleReader migratedScheduleReader;

  @Autowired
  private PearsWorkProgrammeWriter pearsWorkProgrammeWriter;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private Licence licence;

  private LicenceSchedule licenceSchedule;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(9247)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9247")
        .withLicenceReference("P9247")
        .build();
    licenceSchedule = transactionOperations.execute(status -> {
      entityManager.persist(licence);
      return migratedScheduleFixture.migrate(licence);
    });
  }

  @AfterEach
  void tearDown() {
    transactionOperations.executeWithoutResult(status -> {
      migratedScheduleFixture.clear(licence);
      entityManager.remove(entityManager.find(Licence.class, licence.getId()));
    });
  }

  @Test
  void write_whenItFailsPartWay_rollsBackTheWholeLicence() {
    var schedule = migratedScheduleReader.read(licence).orElseThrow().schedule();
    var mapped = PearsWorkProgrammeMapper.map(P9244,
        List.of(programme(List.of(activity("2", "OTHER", "Geological studies", "NOT_STARTED")), List.of())), schedule);
    // A schedule comment with no text breaks its NOT NULL column when the transaction commits, after the activity, its
    // status and its link have been saved, leaving the rollback work to undo
    var broken = new MappedWorkProgramme(mapped.licence(), mapped.activities(), mapped.skipped(),
        List.of(new MappedActivityHistory.Comment(null, LocalDateTime.of(2017, Month.JANUARY, 5, 14, 0))),
        mapped.notes());

    assertThatThrownBy(() -> transactionOperations.execute(
        status -> pearsWorkProgrammeWriter.write(licence, broken, UUID.randomUUID())))
        .isInstanceOf(RuntimeException.class);

    assertThat(count("SELECT count(*) FROM lms.work_programme_activities a "
        + "JOIN lms.schedule_events e ON e.id = a.id WHERE e.licence_schedule_id = :id")).isZero();
    assertThat(count("SELECT count(*) FROM lms.pears_work_programme_migrated_licences "
        + "WHERE licence_schedule_id = :id")).isZero();
    assertThat(count("SELECT count(*) FROM lms.schedule_comments WHERE licence_schedule_id = :id")).isZero();
  }

  private long count(String sql) {
    return ((Number) entityManager.createNativeQuery(sql)
        .setParameter("id", licenceSchedule.getId())
        .getSingleResult()).longValue();
  }
}
