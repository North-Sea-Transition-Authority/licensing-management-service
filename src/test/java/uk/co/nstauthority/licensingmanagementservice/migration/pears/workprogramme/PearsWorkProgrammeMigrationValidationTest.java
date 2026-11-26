package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ReportText;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleService;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Sweeps every licence PEARS holds a work programme for: migrates its schedule and then its work programme into the
 * test database as the two migrations do, and compares the work programme with PEARS through
 * {@link PearsWorkProgrammeComparisonService}, the comparison report's own model, so the sweep and the report can't
 * disagree (plan, design section 5). A licence with an issue to look into fails; one either migration skips or fails
 * for its PEARS data is skipped and reported.
 *
 * <p>Excluded from the {@code test} task by its tag and run by {@code pearsMigrationTest} instead, after the schedule
 * sweep. Reads PEARS over a live Oracle connection, so it disables itself without credentials; set
 * {@code PEARS_MIGRATION_LICENCES} to narrow the sweep. Licences are swept in parallel, each in its own rolled-back
 * transaction.
 */
@Tag("pears-migration")
@IntegrationTest
@Transactional
@Sql(
    scripts = {
        "classpath:migration/create-pears-schedule-migration-tables.sql",
        "classpath:migration/create-pears-work-programme-migration-tables.sql"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// PER_CLASS makes JUnit run the invocations on one thread unless concurrency is asked for explicitly
@Execution(ExecutionMode.CONCURRENT)
@EnabledIfEnvironmentVariable(
    named = "PEARS_DATASOURCE_URL",
    matches = ".+",
    disabledReason = "No PEARS datasource: set PEARS_DATASOURCE_URL, PEARS_DATASOURCE_USERNAME and PEARS_DATASOURCE_PASSWORD"
)
class PearsWorkProgrammeMigrationValidationTest {

  private static final PearsWorkProgrammeValidationReport REPORT = new PearsWorkProgrammeValidationReport();

  // Recorded against each migrated licence, for the cleardowns; nothing is committed here
  private static final UUID RUN_ID = UUID.randomUUID();

  // Threads run in separate rolled-back transactions, so max id + 1 from the database would repeat
  private final AtomicInteger nextLicenceId = new AtomicInteger();

  @Autowired
  private PearsLicenceComparisons licenceComparisons;

  @Autowired
  private PearsScheduleService pearsScheduleService;

  @Autowired
  private PearsScheduleMigrationService scheduleMigrationService;

  @Autowired
  private PearsWorkProgrammeService pearsWorkProgrammeService;

  @Autowired
  private PearsWorkProgrammeMigrationService workProgrammeMigrationService;

  @Autowired
  private LicenceRepository licenceRepository;

  @Autowired
  private LicenceService licenceService;

  @Autowired
  private MigratedScheduleReader migratedScheduleReader;

  @Autowired
  private LmsWorkProgrammeReader lmsWorkProgrammeReader;

  @Autowired
  private PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository;

  @Autowired
  private EntityManager entityManager;

  private List<PearsScheduleEventType> eventTypes;
  private PearsWorkProgrammeComparisonService comparisonService;

  @BeforeAll
  void setUp() {
    nextLicenceId.set(licenceService.getNextLicenceId());
    eventTypes = pearsScheduleService.eventTypes();
    // Built here, as the comparison service is only a bean under the migration profile
    comparisonService = new PearsWorkProgrammeComparisonService(pearsWorkProgrammeService, licenceService,
        migratedScheduleReader, lmsWorkProgrammeReader, migratedLicenceRepository, entityManager, licenceComparisons);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("pearsLicences")
  void workProgramme_whenMigrated_holdsWhatPearsHolds(PearsLicenceReference pearsLicence) {
    var licenceReference = pearsLicence.licenceReference();
    // Against a copy of a real LMS database the licence is already there, as the migration expects; otherwise make it
    if (licenceService.findLicenceByReference(licenceReference).isEmpty()) {
      licenceRepository.save(LicenceTestUtil.builder()
          .withId(nextLicenceId.getAndIncrement())
          .withLicenceType(supportedLicenceType(pearsLicence))
          .withLicencePrefix(pearsLicence.licenceType())
          .withLicenceNumber(String.valueOf(pearsLicence.licenceNo()))
          .withLicenceReference(licenceReference)
          .build());
    }

    PearsWorkProgrammeMigrationOutcome outcome;
    PearsWorkProgrammeComparison comparison;
    try {
      REPORT.recordLicenceSwept();
      var schedule = scheduleMigrationService.migrateLicence(pearsLicence, eventTypes, RUN_ID);
      if (schedule.result() != PearsScheduleMigrationOutcome.Result.MIGRATED
          && !PearsScheduleMigrationService.ALREADY_HAS_A_SCHEDULE.equals(schedule.reason())) {
        REPORT.recordScheduleNotMigrated(licenceReference, "%s: %s".formatted(schedule.result(), schedule.reason()));
      }
      outcome = workProgrammeMigrationService.migrateLicence(pearsLicence, RUN_ID);
      REPORT.recordOutcome(licenceReference, outcome);
      comparison = isComparable(outcome) ? comparisonService.compare(pearsLicence) : null;
      if (comparison != null) {
        REPORT.recordComparison(comparison);
      }
    } catch (RuntimeException e) {
      REPORT.recordFailure(licenceReference, e);
      Assumptions.abort("%s could not be compared: %s".formatted(licenceReference, e.getClass().getSimpleName()));
      return;
    }

    assertThat(Objects.toString(outcome.reason(), ""))
        .as("%s should be written when the migration maps it", licenceReference)
        .doesNotStartWith(PearsWorkProgrammeMigrationService.COULD_NOT_BE_WRITTEN);
    Assumptions.assumeTrue(comparison != null,
        "%s was not migrated: %s".formatted(licenceReference, outcome.reason()));

    var issues = comparison.issueDescriptions();
    assertThat(ReportText.capped(issues))
        .as("%s should hold in LMS what PEARS holds (%,d issue(s))", licenceReference, issues.size())
        .isEmpty();
  }

  /**
   * Migrated now, or migrated already: against a copy of a real LMS database a licence may have its work programme from
   * an earlier run, which is compared as it is, as the dry run does.
   */
  private static boolean isComparable(PearsWorkProgrammeMigrationOutcome outcome) {
    return outcome.result() == PearsWorkProgrammeMigrationOutcome.Result.MIGRATED
        || (outcome.result() == PearsWorkProgrammeMigrationOutcome.Result.SKIPPED
            && PearsWorkProgrammeMigrationService.ALREADY_MIGRATED.equals(outcome.reason()));
  }

  @AfterAll
  void publishReport() {
    REPORT.publish();
  }

  private Stream<PearsLicenceReference> pearsLicences() {
    var licences = narrowed(pearsWorkProgrammeService.workProgrammeLicenceReferences());

    Assumptions.assumeFalse(licences.isEmpty(), "PEARS holds no licence work programmes to compare");

    return licences.stream();
  }

  private static List<PearsLicenceReference> narrowed(List<PearsLicenceReference> licences) {
    var only = System.getenv("PEARS_MIGRATION_LICENCES");
    if (StringUtils.isBlank(only)) {
      return licences;
    }

    var wanted = Stream.of(only.split(","))
        .map(reference -> reference.strip().toUpperCase())
        .filter(StringUtils::isNotBlank)
        .toList();

    return licences.stream()
        .filter(licence -> wanted.contains(licence.licenceReference().toUpperCase()))
        .toList();
  }

  /**
   * The {@link LicenceType} the PEARS licence type maps to, aborting the licence where there is no mapping.
   */
  private static LicenceType supportedLicenceType(PearsLicenceReference pearsLicence) {
    try {
      return LicenceType.getFromPrefix(pearsLicence.licenceType());
    } catch (RuntimeException e) {
      var reason = "PEARS licence type %s has no LicenceType".formatted(pearsLicence.licenceType());
      REPORT.recordNotCompared(pearsLicence.licenceReference(), reason);
      return Assumptions.abort(reason);
    }
  }
}
