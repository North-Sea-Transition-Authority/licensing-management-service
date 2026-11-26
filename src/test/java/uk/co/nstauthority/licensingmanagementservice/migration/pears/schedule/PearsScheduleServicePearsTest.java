package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.zaxxer.hikari.HikariDataSource;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.HashSet;
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
 * Reads schedules out of a real PEARS database with {@link PearsScheduleService}, and checks they come back the shape
 * the migration expects. It checks the reading, not the data: the queries run on Oracle, every version and event comes
 * back in order, and the licences the plan names come back as the data survey found them.
 *
 * <p>Excluded from the {@code test} task by its tag and run by {@code pearsMigrationTest} instead. It needs no Spring
 * context or LMS database, only the PEARS datasource, and disables itself without one. Set
 * {@code PEARS_MIGRATION_LICENCES} to choose the licences read; by default they are the ones the plan uses as examples.
 */
@Tag("pears-migration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "PEARS_DATASOURCE_URL",
    matches = ".+",
    disabledReason = "No PEARS datasource: set PEARS_DATASOURCE_URL, PEARS_DATASOURCE_USERNAME and PEARS_DATASOURCE_PASSWORD"
)
class PearsScheduleServicePearsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsScheduleServicePearsTest.class);

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);

  private static final PearsLicenceReference P1161 = new PearsLicenceReference("P", 1161);

  /**
   * The plan's example licences: the oldest, the one its term and offset examples come from, and the one with no
   * current version.
   */
  private static final List<String> DEFAULT_LICENCES = List.of("P1", "P2244", "P1161");

  /**
   * Every event type the data survey found on live (S1).
   */
  private static final Set<String> KNOWN_EVENT_TYPES = Set.of("P", "T", "R", "MR", "E", "BM", "DD", "2M", "2N", "RD", "PA");

  /**
   * How many PEDL licences to look through for one with {@code RD} events before giving up.
   */
  private static final int RD_LICENCE_SEARCH_LIMIT = 50;

  private HikariDataSource dataSource;

  private PearsScheduleService pearsScheduleService;

  private List<PearsLicenceReference> scheduleLicences;

  @BeforeAll
  void connect() {
    dataSource = PearsTestDataSource.connect();
    pearsScheduleService = new PearsScheduleService(dataSource);
    scheduleLicences = pearsScheduleService.scheduleLicenceReferences();
  }

  @AfterAll
  void disconnect() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @Test
  void checkReadable_theMigrationUserCanReadEveryTableTheMigrationReads() {
    assertThatCode(() -> pearsScheduleService.checkReadable()).doesNotThrowAnyException();
  }

  @Test
  void eventTypes_holdEveryTypeTheSurveyFound() {
    var mnems = pearsScheduleService.eventTypes().stream().map(PearsScheduleEventType::mnem).toList();

    LOGGER.info("PEARS knows {} schedule event types: {}", mnems.size(), mnems);
    assertThat(mnems).containsAll(KNOWN_EVENT_TYPES);
  }

  @Test
  void scheduleLicenceReferences_areInLicenceOrderAndOnlyOfTypesPearsManages() {
    LOGGER.info("PEARS holds a schedule for {} licences on types it manages", scheduleLicences.size());

    assertThat(scheduleLicences).isNotEmpty().doesNotHaveDuplicates();
    assertThat(scheduleLicences)
        .extracting(PearsLicenceReference::licenceType)
        .allMatch(Set.of("P", "PEDL", "PL", "EXL", "ML", "AL", "CE", "DL")::contains);
    assertThat(scheduleLicences).isSortedAccordingTo(
        Comparator.comparing(PearsLicenceReference::licenceType).thenComparingInt(PearsLicenceReference::licenceNo));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("chosenLicences")
  void licenceSchedule_comesBackInOrderWithAtMostOneCurrentVersion(PearsLicenceReference licence) {
    Assumptions.assumeTrue(scheduleLicences.contains(licence), licence + " has no schedule in this PEARS");

    var schedule = pearsScheduleService.licenceSchedule(licence);

    logSummary(schedule);
    assertThat(schedule.versions()).isNotEmpty();
    assertThat(schedule.versions()).extracting(PearsScheduleVersion::plsdId).doesNotHaveDuplicates();
    assertThat(schedule.versions()).isSortedAccordingTo(Comparator.comparing(PearsScheduleVersion::start));
    assertThatCode(schedule::currentVersion).doesNotThrowAnyException();

    for (var version : schedule.versions()) {
      assertThat(version.events())
          .as("events of version %d", version.plsdId())
          .extracting(PearsScheduleEvent::index)
          .allMatch(index -> index >= 1)
          .isSorted()
          .doesNotHaveDuplicates();
    }
  }

  @Test
  void licenceSchedule_p2244StartsOnItsLicenceStartDate() {
    Assumptions.assumeTrue(scheduleLicences.contains(P2244), "P2244 has no schedule in this PEARS");

    var current = pearsScheduleService.licenceSchedule(P2244).currentVersion();

    assertThat(current).hasValueSatisfying(version -> {
      assertThat(version.originDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 1));
      assertThat(version.events()).extracting(PearsScheduleEvent::eventMnem).contains("T", "R", "BM");
    });
  }

  @Test
  void licenceSchedule_p1161HasNoCurrentVersion() {
    Assumptions.assumeTrue(scheduleLicences.contains(P1161), "P1161 has no schedule in this PEARS");

    var schedule = pearsScheduleService.licenceSchedule(P1161);

    assertThat(schedule.versions()).hasSize(2);
    assertThat(schedule.currentVersion()).isEmpty();
  }

  @Test
  void licenceSchedule_areaEventsCarryTheirSubtypeAndArea() {
    var withAreaEvents = scheduleLicences.stream()
        .filter(licence -> licence.licenceType().equals("PEDL"))
        .limit(RD_LICENCE_SEARCH_LIMIT)
        .map(pearsScheduleService::licenceSchedule)
        .filter(schedule -> !areaEvents(schedule).isEmpty())
        .findFirst();

    Assumptions.assumeTrue(withAreaEvents.isPresent(),
        "None of the first %d PEDL licences has RD events".formatted(RD_LICENCE_SEARCH_LIMIT));

    var schedule = withAreaEvents.get();
    var areaEvents = areaEvents(schedule);
    LOGGER.info("{} holds {} RD events in its current version", schedule.licence(), areaEvents.size());

    assertThat(areaEvents).allSatisfy(event -> {
      assertThat(event.eventSubtype()).isNotBlank();
      assertThat(event.area()).isNotNull();
    });
  }

  private Stream<PearsLicenceReference> chosenLicences() {
    var only = System.getenv("PEARS_MIGRATION_LICENCES");
    var wanted = StringUtils.isBlank(only) ? DEFAULT_LICENCES : List.of(only.split(","));

    return wanted.stream()
        .map(reference -> reference.strip().toUpperCase())
        .filter(StringUtils::isNotBlank)
        .map(PearsScheduleServicePearsTest::reference);
  }

  private static List<PearsScheduleEvent> areaEvents(PearsSchedule schedule) {
    return schedule.currentVersion().stream()
        .flatMap(version -> version.events().stream())
        .filter(event -> "RD".equals(event.eventMnem()))
        .toList();
  }

  private static void logSummary(PearsSchedule schedule) {
    var types = new HashSet<String>();
    schedule.versions().forEach(version -> version.events().forEach(event -> types.add(event.eventMnem())));

    LOGGER.info("{}: {} versions, {}, {} events in all, types {}",
        schedule.licence(),
        schedule.versions().size(),
        schedule.currentVersion()
            .map(version -> "current %d starting %s with %d events"
                .formatted(version.plsdId(), version.originDate(), version.events().size()))
            .orElse("no current version"),
        schedule.versions().stream().mapToInt(version -> version.events().size()).sum(),
        types);
  }

  /**
   * A licence reference such as {@code PEDL141}: the letters are the licence type and the digits its number.
   */
  private static PearsLicenceReference reference(String licenceReference) {
    var digits = licenceReference.replaceAll("^[A-Z]+", "");
    return new PearsLicenceReference(licenceReference.substring(0, licenceReference.length() - digits.length()),
        Integer.parseInt(digits));
  }
}
