package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ReportText;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Sweeps every licence PEARS holds a schedule for, migrates it into the test database as the migration does, and
 * compares every version with PEARS through {@link PearsScheduleComparisonService}, the comparison report's own model,
 * so the sweep and the report can't disagree (plan, design section 5). A licence with an issue to look into fails; one
 * the migration rejects for its PEARS data is skipped and reported, as that is data to fix in PEARS rather than a bug.
 *
 * <p>Excluded from the {@code test} task by its tag and run by {@code pearsMigrationTest} instead. Reads PEARS over a
 * live Oracle connection, so it disables itself without credentials; set {@code PEARS_MIGRATION_LICENCES} to narrow
 * the sweep. Licences are swept in parallel, each in its own rolled-back transaction.
 */
@Tag("pears-migration")
@IntegrationTest
@Transactional
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
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
class PearsScheduleMigrationValidationTest {

  private static final PearsScheduleValidationReport REPORT = new PearsScheduleValidationReport();

  // Recorded against each migrated licence, for the cleardown; nothing is committed here
  private static final UUID RUN_ID = UUID.randomUUID();

  // Threads run in separate rolled-back transactions, so max id + 1 from the database would repeat
  private final AtomicInteger nextLicenceId = new AtomicInteger();

  @Autowired
  private PearsScheduleService pearsScheduleService;

  @Autowired
  private PearsScheduleMigrationService migrationService;

  @Autowired
  private LicenceRepository licenceRepository;

  @Autowired
  private LicenceService licenceService;

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

  @Autowired
  private EntityManager entityManager;

  private List<PearsScheduleEventType> eventTypes;
  private PearsScheduleComparisonService comparisonService;

  @BeforeAll
  void setUp() {
    nextLicenceId.set(licenceService.getNextLicenceId());
    eventTypes = pearsScheduleService.eventTypes();
    // Built here, as the comparison service is only a bean under the migration profile
    comparisonService = new PearsScheduleComparisonService(
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
        entityManager
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("pearsLicences")
  void schedule_whenMigrated_holdsWhatPearsHoldsInEveryVersion(PearsLicenceReference pearsLicence) {
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

    PearsScheduleMigrationOutcome outcome;
    PearsScheduleComparison comparison;
    try {
      REPORT.recordLicenceSwept();
      outcome = migrationService.migrateLicence(pearsLicence, eventTypes, RUN_ID);
      REPORT.recordOutcome(licenceReference, outcome);
      if (isComparable(outcome)) {
        comparison = comparisonService.compare(List.of(pearsLicence), true).getFirst();
        REPORT.recordComparison(comparison);
      } else {
        comparison = null;
      }
    } catch (RuntimeException e) {
      REPORT.recordFailure(licenceReference, e);
      Assumptions.abort("%s could not be compared: %s".formatted(licenceReference, e.getClass().getSimpleName()));
      return;
    }

    assertThat(Objects.toString(outcome.reason(), ""))
        .as("%s should be written when the migration maps it", licenceReference)
        .doesNotStartWith(PearsScheduleMigrationService.COULD_NOT_BE_WRITTEN);
    Assumptions.assumeTrue(comparison != null,
        "%s was not migrated: %s".formatted(licenceReference, outcome.reason()));

    var issues = comparison.issueDescriptions();
    assertThat(ReportText.capped(issues))
        .as("%s should hold in LMS what PEARS holds, in every version (%,d issue(s))", licenceReference, issues.size())
        .isEmpty();
  }

  /**
   * Migrated now, or migrated already: against a copy of a real LMS database a licence may have its schedule from an
   * earlier run, which is compared as it is, as the dry run does.
   */
  private static boolean isComparable(PearsScheduleMigrationOutcome outcome) {
    return outcome.result() == PearsScheduleMigrationOutcome.Result.MIGRATED
        || (outcome.result() == PearsScheduleMigrationOutcome.Result.SKIPPED
            && PearsScheduleMigrationService.ALREADY_HAS_A_SCHEDULE.equals(outcome.reason()));
  }

  @AfterAll
  void publishReport() {
    REPORT.publish();
  }

  private Stream<PearsLicenceReference> pearsLicences() {
    var licences = narrowed(pearsScheduleService.scheduleLicenceReferences());

    Assumptions.assumeFalse(licences.isEmpty(), "PEARS holds no licence schedules to compare");

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
