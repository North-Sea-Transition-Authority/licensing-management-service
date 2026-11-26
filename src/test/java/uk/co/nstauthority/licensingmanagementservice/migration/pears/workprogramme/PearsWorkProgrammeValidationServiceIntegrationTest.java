package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.activity;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeWriterIntegrationTest.programme;

import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MigratedScheduleFixture;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigratedLicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationOutcome;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Not transactional itself, so the dry run's own transaction sees the migrated schedule committed, and its rollback can
 * be checked.
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
class PearsWorkProgrammeValidationServiceIntegrationTest {

  private static final PearsLicenceReference P9248 = new PearsLicenceReference("P", 9248);

  @Autowired
  private PearsLicenceComparisons licenceComparisons;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionOperations transactionOperations;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private MigratedScheduleFixture migratedScheduleFixture;

  @Autowired
  private MigratedScheduleReader migratedScheduleReader;

  @Autowired
  private LmsWorkProgrammeReader lmsWorkProgrammeReader;

  @Autowired
  private PearsWorkProgrammeWriter pearsWorkProgrammeWriter;

  @Autowired
  private PearsWorkProgrammeMigratedLicenceRepository workProgrammeMigratedLicenceRepository;

  @Autowired
  private PearsScheduleMigratedLicenceRepository scheduleMigratedLicenceRepository;

  @Autowired
  private LicenceService licenceService;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private PearsWorkProgrammeService pearsWorkProgrammeService;

  private PearsWorkProgrammeMigrationService migrationService;

  private PearsWorkProgrammeValidationService validationService;

  private Licence licence;

  private LicenceSchedule licenceSchedule;

  @BeforeEach
  void setUp() {
    pearsWorkProgrammeService = mock(PearsWorkProgrammeService.class);
    migrationService = new PearsWorkProgrammeMigrationService(pearsWorkProgrammeService, migratedScheduleReader,
        pearsWorkProgrammeWriter, workProgrammeMigratedLicenceRepository, scheduleMigratedLicenceRepository,
        licenceService, transactionOperations);
    var comparisonService = new PearsWorkProgrammeComparisonService(pearsWorkProgrammeService, licenceService,
        migratedScheduleReader, lmsWorkProgrammeReader, workProgrammeMigratedLicenceRepository, entityManager,
        licenceComparisons);
    validationService = new PearsWorkProgrammeValidationService(migrationService, comparisonService, transactionManager);

    licence = LicenceTestUtil.builder()
        .withId(9248)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9248")
        .withLicenceReference("P9248")
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
  void validate_migratesComparesAndRollsBack() {
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P9248)).thenReturn(List.of(withLicence(programme(List.of(
        activity("1", "DRILL_OR_DROP_WELL", "Drill or drop well to 3,000m", "DROPPED"),
        activity("2", "OTHER", "Geological studies", "IN_PROGRESS")
    ), List.of()))));

    var outcome = validationService.validate("P9248", UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.MATCHED);
    assertThat(outcome.reason()).isNull();
    assertThat(outcome.notes()).containsEntry("Result: Match", 2);
    assertThat(count("SELECT count(*) FROM lms.work_programme_activities a "
        + "JOIN lms.schedule_events e ON e.id = a.id WHERE e.licence_schedule_id = :id")).isZero();
    assertThat(count("SELECT count(*) FROM lms.pears_work_programme_migrated_licences "
        + "WHERE licence_schedule_id = :id")).isZero();
  }

  @Test
  void validate_whenTheMigrationFailsTheLicence_recordsWhy() {
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P9248)).thenReturn(List.of(withLicence(programme(
        List.of(activity("1", "MYSTERY", "Unknown", "NOT_STARTED")), List.of()))));

    var outcome = validationService.validate("P9248", UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.FAILED);
    assertThat(outcome.reason()).contains("MYSTERY");
  }

  @Test
  void validate_whenTheWorkProgrammeIsAlreadyMigrated_comparesItAsItIs() {
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P9248)).thenReturn(List.of(withLicence(programme(List.of(
        activity("1", "DRILL_OR_DROP_WELL", "Drill or drop well to 3,000m", "DROPPED"),
        activity("2", "OTHER", "Geological studies", "IN_PROGRESS")
    ), List.of()))));
    migrationService.migrateLicence(P9248, UUID.randomUUID());

    var outcome = validationService.validate("P9248", UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.MATCHED);
    assertThat(outcome.notes())
        .containsEntry(PearsWorkProgrammeValidationService.COMPARED_AS_IT_IS, 1)
        .containsEntry("Result: Match", 2);
    assertThat(count("SELECT count(*) FROM lms.work_programme_activities a "
        + "JOIN lms.schedule_events e ON e.id = a.id WHERE e.licence_schedule_id = :id")).isEqualTo(2);
  }

  private static PearsWorkProgramme withLicence(PearsWorkProgramme programme) {
    return new PearsWorkProgramme(P9248, programme.pedWpId(), programme.pedWpdId(), programme.start(),
        programme.type(), programme.title(), programme.status(), programme.breakpoint(), programme.activities(),
        programme.notes());
  }

  private long count(String sql) {
    return ((Number) entityManager.createNativeQuery(sql)
        .setParameter("id", licenceSchedule.getId())
        .getSingleResult()).longValue();
  }
}
