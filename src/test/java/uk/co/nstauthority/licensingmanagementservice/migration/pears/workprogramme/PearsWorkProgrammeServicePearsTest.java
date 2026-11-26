package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsTestDataSource;

/**
 * Reads work programmes out of a real PEARS database with {@link PearsWorkProgrammeService}, and checks they come back
 * the shape the migration expects: the queries run on Oracle, and activities and notes come back in order, attached to
 * the right programme.
 *
 * <p>Run by {@code pearsMigrationTest}, and disabled without a PEARS datasource. Set {@code PEARS_MIGRATION_LICENCES} to
 * choose the licences read; by default they are ones the plan uses as examples.
 */
@Tag("pears-migration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "PEARS_DATASOURCE_URL",
    matches = ".+",
    disabledReason = "No PEARS datasource: set PEARS_DATASOURCE_URL, PEARS_DATASOURCE_USERNAME and PEARS_DATASOURCE_PASSWORD"
)
class PearsWorkProgrammeServicePearsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsWorkProgrammeServicePearsTest.class);

  /**
   * A flat-list licence, two phase-organised ones (WP-S1f), one with two INITIAL programmes and a {@code LEGACY} one
   * (WP-S1c), and the one with the most versions to read (WP-S14c).
   */
  private static final List<String> DEFAULT_LICENCES = List.of("P2244", "P2614", "P2626", "PEDL180", "P2350");

  private static final Set<String> PROGRAMME_TYPES = Set.of("INITIAL", "FALLOW", "EXTENSION");

  /**
   * How many licences to look through for one with a phase-organised programme before giving up.
   */
  private static final int PHASE_LICENCE_SEARCH_LIMIT = 300;

  private HikariDataSource dataSource;

  private PearsWorkProgrammeService pearsWorkProgrammeService;

  private List<PearsLicenceReference> workProgrammeLicences;

  @BeforeAll
  void connect() {
    dataSource = PearsTestDataSource.connect();
    pearsWorkProgrammeService = new PearsWorkProgrammeService(dataSource);
    workProgrammeLicences = pearsWorkProgrammeService.workProgrammeLicenceReferences();
  }

  @AfterAll
  void disconnect() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  void checkReadable_theMigrationUserCanReadEveryTableTheMigrationReads() {
    assertThatCode(() -> pearsWorkProgrammeService.checkReadable()).doesNotThrowAnyException();
  }

  @Test
  void workProgrammeLicenceReferences_areInLicenceOrderWithoutDuplicates() {
    LOGGER.info("PEARS holds a work programme for {} licences on types it manages", workProgrammeLicences.size());

    assertThat(workProgrammeLicences).isNotEmpty().doesNotHaveDuplicates().isSortedAccordingTo(
        Comparator.comparing(PearsLicenceReference::licenceType).thenComparingInt(PearsLicenceReference::licenceNo));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("chosenLicences")
  void licenceWorkProgrammes_comeBackInOrderWithTheirActivitiesAndNotes(PearsLicenceReference licence) {
    Assumptions.assumeTrue(workProgrammeLicences.contains(licence), licence + " has no work programme in this PEARS");

    var workProgrammes = pearsWorkProgrammeService.licenceWorkProgrammes(licence);

    workProgrammes.forEach(PearsWorkProgrammeServicePearsTest::logSummary);
    assertThat(workProgrammes).isNotEmpty();
    assertThat(workProgrammes).extracting(PearsWorkProgramme::pedWpId).isSorted().doesNotHaveDuplicates();
    assertThat(workProgrammes).allSatisfy(workProgramme -> assertShape(licence, workProgramme));
  }

  @Test
  void licenceWorkProgrammes_phaseActivitiesNameTheirPhase() {
    var withPhases = workProgrammeLicences.stream()
        .filter(licence -> licence.licenceType().equals("P"))
        .sorted(Comparator.comparingInt(PearsLicenceReference::licenceNo).reversed())
        .limit(PHASE_LICENCE_SEARCH_LIMIT)
        .flatMap(licence -> pearsWorkProgrammeService.licenceWorkProgrammes(licence).stream())
        .filter(workProgramme -> workProgramme.activities().stream()
            .anyMatch(activity -> activity.listKind() == PearsActivity.ListKind.PHASE))
        .findFirst();

    Assumptions.assumeTrue(withPhases.isPresent(),
        "None of the latest %d P licences has a phase-organised programme".formatted(PHASE_LICENCE_SEARCH_LIMIT));

    var workProgramme = withPhases.get();
    logSummary(workProgramme);
    assertThat(workProgramme.activities())
        .filteredOn(activity -> activity.listKind() == PearsActivity.ListKind.PHASE)
        .allSatisfy(activity -> assertThat(activity.phaseType()).startsWith("Phase "));
  }

  private static void assertShape(PearsLicenceReference licence, PearsWorkProgramme workProgramme) {
    assertThat(workProgramme.licence()).isEqualTo(licence);
    assertThat(workProgramme.type()).isIn(PROGRAMME_TYPES);
    assertThat(workProgramme.status()).isNotEqualTo("DRAFT");

    assertThat(workProgramme.activities())
        .extracting(PearsActivity::position)
        .isSorted()
        .doesNotHaveDuplicates();
    assertThat(workProgramme.activities()).allSatisfy(activity -> {
      assertThat(activity.activityId()).isNotBlank();
      assertThat(activity.phaseType() == null).isEqualTo(activity.listKind() == PearsActivity.ListKind.FLAT);
      // The current version holds the activity with its current status, so the earliest such version always exists.
      assertThat(activity.statusSince()).isNotNull().isBeforeOrEqualTo(workProgramme.start());
    });

    assertThat(workProgramme.notes())
        .extracting(PearsNote::position)
        .isSorted()
        .doesNotHaveDuplicates();
    // WP-S9: every note on live has a date, so one that comes back null was not read.
    assertThat(workProgramme.notes()).allSatisfy(note -> assertThat(note.datetime()).isNotNull());
  }

  private Stream<PearsLicenceReference> chosenLicences() {
    var only = System.getenv("PEARS_MIGRATION_LICENCES");
    var wanted = StringUtils.isBlank(only) ? DEFAULT_LICENCES : List.of(only.split(","));

    return wanted.stream()
        .map(reference -> reference.strip().toUpperCase())
        .filter(StringUtils::isNotBlank)
        .map(PearsLicenceReference::parse);
  }

  private static void logSummary(PearsWorkProgramme workProgramme) {
    LOGGER.info("{} programme {} ({}, {}): version {} from {}, {} activities ({} in phases), {} notes, breakpoint {}",
        workProgramme.licence(),
        workProgramme.pedWpId(),
        workProgramme.type(),
        workProgramme.status(),
        workProgramme.pedWpdId(),
        workProgramme.start(),
        workProgramme.activities().size(),
        workProgramme.activities().stream().filter(activity -> activity.listKind() == PearsActivity.ListKind.PHASE).count(),
        workProgramme.notes().size(),
        workProgramme.breakpoint());
  }
}
