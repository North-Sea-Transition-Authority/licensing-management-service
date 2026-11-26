package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.event;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.term;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleWriterIntegrationTest.EVENT_TYPES;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRate;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceComparisons;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Result;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleComparison.Row;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

/**
 * Migrates the writer test's two-version schedule (P2244's shape) with the real writer, changes LMS here and there, and
 * compares, so every result the report gives turns up.
 */
@Sql(
    scripts = "classpath:migration/create-pears-schedule-migration-tables.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS,
    config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
@Transactional
@IntegrationTest
class PearsScheduleComparisonServiceIntegrationTest {

  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);
  private static final PearsLicenceReference LICENCE = new PearsLicenceReference("P", 9244);

  @Autowired
  private PearsLicenceComparisons licenceComparisons;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PearsScheduleWriter pearsScheduleWriter;

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

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private final PearsScheduleService pearsScheduleService = mock(PearsScheduleService.class);

  private Licence licence;
  private PearsScheduleComparisonService comparisonService;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder()
        .withId(9244)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("9244")
        .withLicenceReference("P9244")
        .build();
    entityManager.persist(licence);

    when(pearsScheduleService.eventTypes()).thenReturn(EVENT_TYPES);
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
        entityManager,
        licenceComparisons
    );
  }

  @Test
  void migratedLicences_isEveryLicenceTheMigrationWroteASchedule() {
    assertThat(comparisonService.migratedLicences()).isEmpty();

    migrate(PearsScheduleWriterIntegrationTest.pearsSchedule());

    assertThat(comparisonService.migratedLicences()).containsExactly(LICENCE);
  }

  @Test
  void compare_whenMigratedAsItIs_matchesEveryEvent() {
    migrate(PearsScheduleWriterIntegrationTest.pearsSchedule());

    var comparison = compareOne(false);

    assertThat(comparison.problem()).isNull();
    assertThat(comparison.versions()).singleElement().satisfies(version -> {
      assertThat(version.pearsPlsdId()).isEqualTo(2L);
      assertThat(version.lmsStatus()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
      assertThat(version.note()).isNull();
    });
    // The yearly rate carries on into the Second and Third terms as LMS rates of their own
    assertThat(comparison.resultCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(
        Result.MATCH, 9,
        Result.CARRIED_ON, 2
    ));
    assertThat(rows(comparison))
        .extracting(Row::event, Row::caption, Row::pearsDate, Row::lmsDate, Row::result)
        .startsWith(
            tuple("Start", "Schedule start", START, START, Result.MATCH),
            tuple("Rate", "In Initial Term", START, START, Result.MATCH),
            tuple("Term", "Initial Term ends", LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2018, Month.DECEMBER, 1),
                Result.MATCH)
        );
  }

  @Test
  void compare_withEveryVersion_pairsEachPearsVersionWithTheDetailMigratedFromIt() {
    migrate(PearsScheduleWriterIntegrationTest.pearsSchedule());

    var comparison = compareOne(true);

    assertThat(comparison.versions())
        .extracting(PearsScheduleComparison.Version::pearsPlsdId, PearsScheduleComparison.Version::lmsStatus)
        .containsExactly(
            tuple(1L, LicenceScheduleDetailStatus.REPLACED),
            tuple(2L, LicenceScheduleDetailStatus.ACTIVE)
        );
    assertThat(comparison.versions().getFirst().rows())
        .extracting(Row::result)
        .containsOnly(Result.MATCH, Result.CARRIED_ON);
    assertThat(comparison.resultCounts()).doesNotContainKey(Result.ORIGINAL_EVENT_DIFFERS);
  }

  @Test
  void compare_whenLmsWasChangedAfterTheMigration_showsEachDifference() {
    migrate(PearsScheduleWriterIntegrationTest.pearsSchedule());
    var otherEvents = activeEvents(OtherScheduleEvent.class);
    var drillOrDrop = otherEvents.stream().filter(event -> event.getOtherCategoryName() != null).findFirst().orElseThrow();
    var relinquishment = otherEvents.stream().filter(event -> event.getOtherCategoryName() == null).findFirst().orElseThrow();
    var firstRate = activeEvents(LicenceScheduleRate.class).stream()
        .filter(rate -> START.equals(rate.getStartDate()))
        .findFirst()
        .orElseThrow();

    drillOrDrop.setEventDate(drillOrDrop.getEventDate().plusDays(3));
    firstRate.setRentalRate(new BigDecimal("175"));
    // An unlinked event is LMS's own, and its PEARS event is then missing
    eventMigrationLinkRepository.deleteById(relinquishment.getId());
    entityManager.flush();
    entityManager.clear();

    var rows = rows(compareOne(false));

    assertThat(rows)
        .filteredOn(row -> row.result() != Result.MATCH && row.result() != Result.CARRIED_ON)
        .extracting(Row::event, Row::result, Row::resultText)
        .containsExactlyInAnyOrder(
            tuple("Rate", Result.VALUE_DIFFERS, "Value differs"),
            tuple("Mandatory relinquishment", Result.MISSING_IN_LMS, "Missing in LMS"),
            tuple("Mandatory relinquishment", Result.LMS_ONLY, "LMS only"),
            tuple("Other activity", Result.DIFFERS, "Differs by +3 days")
        );
  }

  @Test
  void compare_withEveryVersion_whenAnEventIsNotTheSameEventAsInTheVersionBefore_saysSo() {
    migrate(PearsScheduleWriterIntegrationTest.pearsSchedule());
    var activeInitialTerm = activeEvents(LicenceScheduleTerm.class).stream()
        .filter(term -> term.getStartDate().equals(START))
        .findFirst()
        .orElseThrow();
    activeInitialTerm.setOriginalEventId(UUID.randomUUID());
    entityManager.flush();
    entityManager.clear();

    var comparison = compareOne(true);

    assertThat(comparison.versions().getLast().rows())
        .filteredOn(row -> row.result() == Result.ORIGINAL_EVENT_DIFFERS)
        .extracting(Row::caption)
        .containsExactly("Initial Term ends");
    assertThat(comparison.versions().getFirst().rows())
        .extracting(Row::result)
        .doesNotContain(Result.ORIGINAL_EVENT_DIFFERS);
  }

  @Test
  void compare_whenPearsHasSavedAVersionSinceTheMigration_saysWhichVersionLmsCameFrom() {
    var migrated = PearsScheduleWriterIntegrationTest.pearsSchedule();
    migrate(migrated);
    var second = migrated.versions().getLast();
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(schedule(
        migrated.versions().getFirst(),
        new PearsScheduleVersion(second.plsdId(), second.start(), LocalDateTime.of(2026, Month.JUNE, 8, 9, 0),
            second.originDate(), second.events()),
        version(3, null, START,
            term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
            event("E", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0))
    ));

    var version = compareOne(false).versions().getFirst();

    assertThat(version.pearsPlsdId()).isEqualTo(3L);
    assertThat(version.note()).isEqualTo(PearsScheduleComparisonService.FROM_ANOTHER_VERSION.formatted(2L));
    // LMS's events came from PEARS, just from the version before
    assertThat(version.rows())
        .filteredOn(row -> row.result() == Result.LMS_ONLY)
        .isNotEmpty()
        .extracting(Row::detail)
        .containsOnly(PearsScheduleVersionComparer.FROM_ANOTHER_VERSION.formatted(2L));
  }

  @Test
  void compare_whenTheMigrationSkipsTheLicence_showsItsEventsAsNotMigratedRatherThanMissing() {
    when(pearsScheduleService.eventTypes()).thenReturn(Stream.concat(
        EVENT_TYPES.stream(), Stream.of(new PearsScheduleEventType("BM", "Bespoke Milestone", 6))).toList());
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(schedule(version(1, null, START,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("E", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0).withComments("Well drilled")
    )));

    var comparison = compareOne(false);

    assertThat(comparison.migrationFailure()).isEqualTo(PearsScheduleMigrationService.NEEDS_BESPOKE_MILESTONE);
    assertThat(rows(comparison)).extracting(Row::result).containsOnly(Result.NOT_MIGRATED);
  }

  @Test
  void compare_whenTheMigrationLeavesEventsOut_saysWhy() {
    migrate(schedule(version(1, null, START,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        term(null, START),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150"),
        event("R", LocalDate.of(2019, Month.DECEMBER, 1)).withOffset(5, 0, 0).withYearlyRate("300"),
        event(null, LocalDate.of(2016, Month.DECEMBER, 1)).withComments("No type")
    )));

    var rows = rows(compareOne(false));

    assertThat(rows)
        .filteredOn(row -> row.result() != Result.MATCH)
        .extracting(Row::event, Row::result, Row::detail)
        .containsExactlyInAnyOrder(
            tuple("Term", Result.NOT_MIGRATED, PearsScheduleVersionComparer.UNNUMBERED_TERM),
            tuple("Rate", Result.NOT_MIGRATED, PearsScheduleVersionComparer.LEFT_OUT_BY_RULES),
            tuple("Untyped", Result.NOT_MIGRATED, PearsScheduleVersionComparer.UNTYPED),
            tuple("Expiry", Result.NO_EXPIRY, null)
        );
  }

  @Test
  void compare_withEveryVersion_whenAVersionHasNoStartDate_showsItsDatesAsFromTheOffsets() {
    var undated = version(1, LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0), null,
        // PEARS's 1900 base, as it dates a version with no start date
        term("1", LocalDate.of(1904, Month.JANUARY, 1)).withOffset(4, 0, 0),
        event("E", LocalDate.of(1904, Month.JANUARY, 1)).withOffset(4, 0, 0)
    );
    var current = version(2, null, START,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("E", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0)
    );
    migrate(schedule(undated, current));

    var comparison = compareOne(true);

    assertThat(comparison.versions().getFirst().rows())
        .extracting(Row::event, Row::lmsDate, Row::result)
        .containsExactly(
            tuple("Start", START, Result.NO_START_DATE),
            tuple("Term", LocalDate.of(2018, Month.DECEMBER, 1), Result.FROM_OFFSET),
            tuple("Expiry", LocalDate.of(2018, Month.DECEMBER, 1), Result.FROM_OFFSET)
        );
  }

  @Test
  void compare_withEveryVersion_whenAnEventDatedFromItsOffsetDiffers_measuresItFromTheDateExpected() {
    var undated = version(1, LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0), null,
        term("1", LocalDate.of(1904, Month.JANUARY, 1)).withOffset(4, 0, 0),
        event("E", LocalDate.of(1904, Month.JANUARY, 1)).withOffset(4, 0, 0)
    );
    var current = version(2, null, START,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("E", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0)
    );
    migrate(schedule(undated, current));
    entityManager.createNativeQuery("""
            UPDATE lms.licence_schedule_expiry_dates x SET expiry_date = x.expiry_date + 3
            FROM lms.licence_schedule_details d
            WHERE d.id = x.licence_schedule_detail_id AND d.status = 'REPLACED'""")
        .executeUpdate();

    var comparison = compareOne(true);

    // Not from PEARS's 1904 date, which isn't used
    assertThat(comparison.versions().getFirst().rows())
        .filteredOn(row -> row.result() == Result.DIFFERS)
        .extracting(Row::event, Row::comparedWith, Row::resultText)
        .containsExactly(tuple("Expiry", LocalDate.of(2018, Month.DECEMBER, 1), "Differs by +3 days from the date expected"));
  }

  @Test
  void compare_whenTheLicenceHasNoLmsSchedule_showsEveryPearsEventAsMissing() {
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(PearsScheduleWriterIntegrationTest.pearsSchedule());

    var version = compareOne(false).versions().getFirst();

    assertThat(version.note()).isEqualTo(PearsScheduleComparisonService.NO_LMS_SCHEDULE);
    assertThat(version.rows()).extracting(Row::result).containsOnly(Result.MISSING_IN_LMS);
  }

  @Test
  void compare_whenTheMigrationFailsTheLicence_showsItsEventsAsNotMigrated() {
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(schedule(version(1, null, START,
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150")
    )));

    var comparison = compareOne(false);

    assertThat(comparison.migrationFailure()).isNotNull();
    assertThat(comparison.versions().getFirst().note()).isNull();
    assertThat(rows(comparison)).extracting(Row::result).containsOnly(Result.NOT_MIGRATED, Result.NO_EXPIRY);
  }

  @Test
  void compare_whenTheLicenceIsNotInLms_saysSo() {
    var comparison = comparisonService.compare(List.of(new PearsLicenceReference("P", 9999)), false).getFirst();

    assertThat(comparison.problem()).isEqualTo(PearsScheduleMigrationService.NOT_IN_LMS);
    assertThat(comparison.versions()).isEmpty();
  }

  @Test
  void compare_whenPearsHoldsNoSchedule_saysSo() {
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(new PearsSchedule(LICENCE, List.of()));

    assertThat(compareOne(false).problem()).isEqualTo(PearsScheduleComparisonService.NO_PEARS_SCHEDULE);
  }

  private void migrate(PearsSchedule pearsSchedule) {
    when(pearsScheduleService.licenceSchedule(any())).thenReturn(pearsSchedule);
    var mappedSchedule = new PearsScheduleMapper().map(pearsSchedule, EVENT_TYPES);
    pearsScheduleWriter.write(licence, mappedSchedule, pearsSchedule, UUID.randomUUID()).orElseThrow();
    entityManager.flush();
    entityManager.clear();
  }

  private PearsScheduleComparison compareOne(boolean allVersions) {
    return comparisonService.compare(List.of(LICENCE), allVersions).getFirst();
  }

  private static List<Row> rows(PearsScheduleComparison comparison) {
    return comparison.versions().stream().flatMap(version -> version.rows().stream()).toList();
  }

  private <T> List<T> activeEvents(Class<T> type) {
    return entityManager.createQuery(
            "FROM %s WHERE licenceScheduleDetail.status = :status".formatted(type.getSimpleName()), type)
        .setParameter("status", LicenceScheduleDetailStatus.ACTIVE)
        .getResultList();
  }

  private static PearsSchedule schedule(PearsScheduleVersion... versions) {
    return new PearsSchedule(LICENCE, List.of(versions));
  }

  private static PearsScheduleVersion version(
      long plsdId,
      LocalDateTime end,
      LocalDate originDate,
      PearsEventFixture... events
  ) {
    return new PearsScheduleVersion(
        plsdId,
        LocalDateTime.of(2014 + (int) plsdId, Month.JANUARY, 1, 9, 0),
        end,
        originDate,
        Stream.of(events).map(PearsEventFixture::build).toList()
    );
  }
}
