package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.event;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleWriterIntegrationTest.EVENT_TYPES;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Dry runs the writer test's P2244-shaped schedule. Not in a test transaction, as the point is that the dry run's own
 * transaction keeps nothing: what the tests set up is committed, and cleared away after each.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@IntegrationTest
class PearsScheduleValidationServiceIntegrationTest {

  private static final int LICENCE_ID = 9247;
  private static final String LICENCE_REFERENCE = "P9247";
  private static final PearsLicenceReference LICENCE = new PearsLicenceReference("P", 9247);

  @Autowired
  private PearsLicenceComparisons licenceComparisons;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private PearsScheduleWriter pearsScheduleWriter;

  @Autowired
  private LicenceService licenceService;

  @Autowired
  private LicenceScheduleService licenceScheduleService;

  @Autowired
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Autowired
  private LicenceStartDateRepository licenceStartDateRepository;

  @Autowired
  private LicenceScheduleTermService licenceScheduleTermService;

  @Autowired
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Autowired
  private LicenceScheduleRateService licenceScheduleRateService;

  @Autowired
  private OtherScheduleEventService otherScheduleEventService;

  @Autowired
  private LicenceScheduleExpiryService licenceScheduleExpiryService;

  @Autowired
  private LicenceScheduleAreaEventService licenceScheduleAreaEventService;

  @Autowired
  private PearsScheduleEventMigrationLinkRepository eventMigrationLinkRepository;

  @Autowired
  private PearsScheduleMigratedLicenceRepository migratedLicenceRepository;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private final PearsScheduleService pearsScheduleService = mock(PearsScheduleService.class);

  private TransactionTemplate transactionTemplate;
  private PearsScheduleMigrationService migrationService;
  private PearsScheduleValidationService validationService;

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.executeWithoutResult(status -> entityManager.persist(LicenceTestUtil.builder()
        .withId(LICENCE_ID)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber(String.valueOf(LICENCE_ID))
        .withLicenceReference(LICENCE_REFERENCE)
        .build()));

    when(pearsScheduleService.licenceSchedule(any())).thenReturn(PearsScheduleWriterIntegrationTest.pearsSchedule());
    migrationService = new PearsScheduleMigrationService(
        pearsScheduleService, pearsScheduleWriter, licenceService, licenceScheduleService, transactionTemplate);
    var comparisonService = new PearsScheduleComparisonService(
        pearsScheduleService,
        licenceService,
        licenceScheduleDetailService,
        new LmsScheduleVersionReader(
            licenceStartDateRepository,
            licenceScheduleTermService,
            licenceSchedulePhaseService,
            licenceScheduleRateService,
            otherScheduleEventService,
            licenceScheduleExpiryService,
            licenceScheduleAreaEventService,
            eventMigrationLinkRepository
        ),
        migratedLicenceRepository,
        entityManager,
        licenceComparisons
    );
    validationService = new PearsScheduleValidationService(migrationService, comparisonService, transactionManager);
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.execute(status -> new PearsScheduleCleardownService(migratedLicenceRepository, entityManager)
        .clear(LICENCE_REFERENCE));
    jdbcTemplate.update("DELETE FROM lms.licences_aud WHERE id = ?", LICENCE_ID);
    jdbcTemplate.update("DELETE FROM lms.licences WHERE id = ?", LICENCE_ID);
  }

  @Test
  void validate_whenTheMigrationHoldsWhatPearsHolds_matchesAndKeepsNothing() {
    var outcome = validationService.validate(LICENCE_REFERENCE, EVENT_TYPES, UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.MATCHED);
    assertThat(outcome.reason()).isNull();
    // Every version: 7 events and the start in the first, 9 in the second, and the yearly rate carried on 3 times
    assertThat(outcome.notes())
        .containsEntry("Result: Match", 16)
        .containsEntry("Result: Carried on", 3);
    assertThat(schedulesKept()).isZero();
    assertThat(migratedLicenceRepository.count()).isZero();
  }

  @Test
  void validate_whenTheLicenceAlreadyHasASchedule_comparesItAsItIs() {
    migrateForReal();

    var outcome = validationService.validate(LICENCE_REFERENCE, EVENT_TYPES, UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.MATCHED);
    assertThat(outcome.notes()).containsEntry(PearsScheduleValidationService.COMPARED_AS_IT_IS, 1);
    assertThat(schedulesKept()).isOne();
  }

  @Test
  void validate_whenLmsDiffersFromPears_listsTheIssues() {
    migrateForReal();
    jdbcTemplate.update("""
        UPDATE lms.other_schedule_events SET event_date = event_date + 3
        WHERE id IN (SELECT schedule_event_id FROM lms.pears_schedule_event_migration_links WHERE pears_event_mnem = 'MR')""");

    var outcome = validationService.validate(LICENCE_REFERENCE, EVENT_TYPES, UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.DIFFERS);
    assertThat(outcome.reason())
        .contains("version 1, Mandatory relinquishment: PEARS 2018-12-01 50, LMS 2018-12-04 50: Differs by +3 days")
        .contains("version 2, Mandatory relinquishment");
    assertThat(outcome.notes()).containsEntry("Result: Differs", 2);
  }

  @Test
  void validate_whenTheMigrationFailsTheLicence_saysWhyAndKeepsNothing() {
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(new PearsSchedule(LICENCE, List.of(
        new PearsScheduleVersion(1L, LocalDateTime.of(2019, Month.JANUARY, 1, 9, 0), null,
            LocalDate.of(2014, Month.DECEMBER, 1),
            List.of(event("R", LocalDate.of(2014, Month.DECEMBER, 1)).withOffset(0, 0, 0).withYearlyRate("150").build()))
    )));

    var outcome = validationService.validate(LICENCE_REFERENCE, EVENT_TYPES, UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.FAILED);
    assertThat(outcome.reason()).isNotBlank();
    assertThat(schedulesKept()).isZero();
  }

  @Test
  void validate_whenTheLicenceIsNotInLms_skipsIt() {
    var outcome = validationService.validate("P9999", EVENT_TYPES, UUID.randomUUID());

    assertThat(outcome.result()).isEqualTo(PearsScheduleValidationOutcome.Result.SKIPPED);
    assertThat(outcome.reason()).isEqualTo(PearsScheduleMigrationService.NOT_IN_LMS);
  }

  private void migrateForReal() {
    var outcome = migrationService.migrateLicence(LICENCE, EVENT_TYPES, UUID.randomUUID());
    assertThat(outcome.result()).isEqualTo(PearsScheduleMigrationOutcome.Result.MIGRATED);
  }

  private long schedulesKept() {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM lms.licence_schedules WHERE licence_id = ?", Long.class, LICENCE_ID);
  }
}
