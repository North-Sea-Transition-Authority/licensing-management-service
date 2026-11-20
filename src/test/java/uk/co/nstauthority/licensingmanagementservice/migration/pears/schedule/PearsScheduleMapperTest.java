package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Maps fixture schedules shaped like the licences the plan and data survey use as examples. Where the survey gives a
 * licence's dates they are used as they are; any other date is illustrative and says so.
 */
@ExtendWith(MockitoExtension.class)
class PearsScheduleMapperTest {

  /**
   * The PEARS event type catalogue on live (data survey S1).
   */
  private static final List<PearsScheduleEventType> EVENT_TYPES = List.of(
      new PearsScheduleEventType("P", "Phase", -4),
      new PearsScheduleEventType("T", "Term", -3),
      new PearsScheduleEventType("R", "Rate", -2),
      new PearsScheduleEventType("MR", "Mandatory Relinquishment", -1),
      new PearsScheduleEventType("E", "Licence Expiry Date", 1),
      new PearsScheduleEventType("PA", "Prospective Area", 5),
      new PearsScheduleEventType("BM", "Bespoke Milestone", 6),
      new PearsScheduleEventType("DD", "Drill-or-drop", 7),
      new PearsScheduleEventType("2M", "Second Term with autosurrender", 8),
      new PearsScheduleEventType("2N", "2nd term with all acreage", 9),
      new PearsScheduleEventType("RD", "Retention/Development Area", 10)
  );

  private static final LocalDateTime SAVED = LocalDateTime.of(2024, Month.JUNE, 3, 14, 30);

  private final PearsScheduleMapper mapper = new PearsScheduleMapper();

  private int nextEventIndex = 1;

  @Test
  void map_p2244_threeTermsEndingOnTheExpiry() {
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)),
        term("3", LocalDate.of(2040, Month.DECEMBER, 1)),
        event("BM", LocalDate.of(2023, Month.NOVEMBER, 30)),
        event("BM", LocalDate.of(2024, Month.MARCH, 31)),
        yearlyRate("150", LocalDate.of(2018, Month.DECEMBER, 1)),
        expiry(LocalDate.of(2040, Month.DECEMBER, 1), " Licence expires ")
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    var version = mapped.activeVersion();
    assertThat(version.plsdId()).isEqualTo(1);
    assertThat(version.status()).isEqualTo(LicenceScheduleDetailStatus.ACTIVE);
    assertThat(version.createdInstant()).isEqualTo(SAVED.atZone(ZoneId.of("Europe/London")).toInstant());
    assertThat(version.startDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 1));
    assertThat(version.startDateBorrowed()).isFalse();
    assertThat(version.datesFromOffsets()).isFalse();
    assertThat(version.terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate, MappedTerm::duration)
        .containsExactly(
            tuple(1, LocalDate.of(2014, Month.DECEMBER, 1), LocalDate.of(2018, Month.NOVEMBER, 30), years(4)),
            tuple(2, LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2028, Month.NOVEMBER, 30), years(10)),
            tuple(3, LocalDate.of(2028, Month.DECEMBER, 1), LocalDate.of(2040, Month.NOVEMBER, 30), years(12))
        );
    assertThat(version.terms()).extracting(term -> term.source().termNo()).containsExactly("1", "2", "3");
    assertThat(version.phases()).isEmpty();
    assertThat(version.expiry()).isNotNull();
    assertThat(version.expiry().pearsDate()).isEqualTo(LocalDate.of(2040, Month.DECEMBER, 1));
    assertThat(version.expiry().comments()).isEqualTo("Licence expires");
    // Illustrative rate: one yearly rate from the second term becomes a term rate in each later term
    assertThat(version.rates())
        .extracting(MappedRate::startDate, MappedRate::termNo, MappedRate::definitionOption)
        .containsExactly(
            tuple(LocalDate.of(2018, Month.DECEMBER, 1), 2, RateDefinitionOption.TERM),
            tuple(LocalDate.of(2028, Month.DECEMBER, 1), 3, RateDefinitionOption.TERM)
        );
    // The fixture's bespoke milestones have no comments (PearsOtherEventMapperTest maps them in full)
    assertThat(mapped.notes().noteCountsByReason())
        .containsExactly(Map.entry(PearsOtherEventMapper.DEFAULT_DESCRIPTION, 2));
  }

  @Test
  void map_p2522_phasesAAndCWithNoB() {
    // P2522's start, phases and initial term; its later terms and expiry are left out
    var schedule = schedule("P", 2522, version(1, LocalDate.of(2020, Month.DECEMBER, 1),
        phase("A", LocalDate.of(2023, Month.DECEMBER, 1)),
        phase("C", LocalDate.of(2024, Month.DECEMBER, 1)),
        term("1", LocalDate.of(2024, Month.DECEMBER, 1))
    ));

    var version = mapper.map(schedule, EVENT_TYPES).activeVersion();

    assertThat(version.terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate, MappedTerm::duration)
        .containsExactly(
            tuple(1, LocalDate.of(2020, Month.DECEMBER, 1), LocalDate.of(2024, Month.NOVEMBER, 30), years(4))
        );
    assertThat(version.phases())
        .extracting(MappedPhase::phaseType, MappedPhase::startDate, MappedPhase::endDate, MappedPhase::duration)
        .containsExactly(
            tuple(PhaseType.PHASE_A, LocalDate.of(2020, Month.DECEMBER, 1), LocalDate.of(2023, Month.NOVEMBER, 30), years(3)),
            tuple(PhaseType.PHASE_C, LocalDate.of(2023, Month.DECEMBER, 1), LocalDate.of(2024, Month.NOVEMBER, 30), years(1))
        );
  }

  @Test
  void map_patternA_secondAndThirdTermsWithNoInitialTerm() {
    // Illustrative dates, shaped like the 2017 to 2024 rounds
    var schedule = schedule("P", 2400, version(1, LocalDate.of(2019, Month.OCTOBER, 1),
        term("2", LocalDate.of(2025, Month.OCTOBER, 1)),
        term("3", LocalDate.of(2043, Month.OCTOBER, 1)),
        expiry(LocalDate.of(2043, Month.OCTOBER, 1), null)
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.activeVersion().terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate)
        .containsExactly(
            tuple(2, LocalDate.of(2019, Month.OCTOBER, 1), LocalDate.of(2025, Month.SEPTEMBER, 30)),
            tuple(3, LocalDate.of(2025, Month.OCTOBER, 1), LocalDate.of(2043, Month.SEPTEMBER, 30))
        );
    assertThat(mapped.notes().noteCountsByReason()).isEmpty();
  }

  @Test
  void map_patternB_expiryLaterThanTheFinalTerm_isMigratedAsItIsAndNoted() {
    // P516's term and expiry; the start date is illustrative
    var schedule = schedule("P", 516, version(1, LocalDate.of(1985, Month.JUNE, 14),
        term("1", LocalDate.of(1991, Month.JUNE, 14)),
        expiry(LocalDate.of(2021, Month.JUNE, 14), null)
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    var version = mapped.activeVersion();
    assertThat(version.terms()).extracting(MappedTerm::termNo, MappedTerm::endDate)
        .containsExactly(tuple(1, LocalDate.of(1991, Month.JUNE, 13)));
    assertThat(version.expiry()).isNotNull();
    assertThat(version.expiry().pearsDate()).isEqualTo(LocalDate.of(2021, Month.JUNE, 14));
    assertThat(mapped.notes().noteCountsByReason())
        .containsExactly(Map.entry(PearsScheduleMapper.EXPIRY_NOT_ON_FINAL_TERM_END, 1));
  }

  @Test
  void map_patternC_fourTerms() {
    // P1484's term dates; the start date is illustrative
    var schedule = schedule("P", 1484, version(1, LocalDate.of(2005, Month.APRIL, 1),
        term("1", LocalDate.of(2009, Month.APRIL, 1)),
        term("2", LocalDate.of(2013, Month.APRIL, 1)),
        term("3", LocalDate.of(2019, Month.APRIL, 1)),
        term("4", LocalDate.of(2037, Month.APRIL, 1)),
        expiry(LocalDate.of(2037, Month.APRIL, 1), null)
    ));

    var version = mapper.map(schedule, EVENT_TYPES).activeVersion();

    assertThat(version.terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::duration)
        .containsExactly(
            tuple(1, LocalDate.of(2005, Month.APRIL, 1), years(4)),
            tuple(2, LocalDate.of(2009, Month.APRIL, 1), years(4)),
            tuple(3, LocalDate.of(2013, Month.APRIL, 1), years(6)),
            tuple(4, LocalDate.of(2019, Month.APRIL, 1), years(18))
        );
    assertThat(version.finalTerm().boundary()).isEqualTo(version.expiry().pearsDate());
  }

  @Test
  void map_patternD_unnumberedTermOnTheStartDate_isIgnoredAndCounted() {
    var schedule = schedule("P", 2313, version(1, LocalDate.of(2017, Month.MAY, 15),
        term(null, LocalDate.of(2017, Month.MAY, 15)),
        term("1", LocalDate.of(2026, Month.MAY, 15)),
        term("2", LocalDate.of(2032, Month.MAY, 15)),
        term("3", LocalDate.of(2050, Month.MAY, 15)),
        expiry(LocalDate.of(2050, Month.MAY, 15), null)
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.activeVersion().terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate)
        .containsExactly(
            tuple(1, LocalDate.of(2017, Month.MAY, 15)),
            tuple(2, LocalDate.of(2026, Month.MAY, 15)),
            tuple(3, LocalDate.of(2032, Month.MAY, 15))
        );
    // An unnumbered term on the start date doesn't make the version's dates stale
    assertThat(mapped.activeVersion().datesFromOffsets()).isFalse();
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(Map.entry(PearsScheduleMapper.UNNUMBERED_TERM, 1));
  }

  @Test
  void map_patternE_noNumberedTerm_isNotMigrated() {
    var schedule = schedule("P", 249, version(1, LocalDate.of(1965, Month.APRIL, 1),
        event("R", LocalDate.of(1965, Month.APRIL, 1)),
        expiry(LocalDate.of(2025, Month.APRIL, 1), null)
    ));

    assertThatThrownBy(() -> mapper.map(schedule, EVENT_TYPES))
        .isInstanceOfSatisfying(PearsScheduleMappingException.class, e -> {
          assertThat(e.licence()).isEqualTo(new PearsLicenceReference("P", 249));
          assertThat(e.reason()).isEqualTo(PearsScheduleMappingException.NO_TERMS);
        });
  }

  @Test
  void map_patternF_initialAndThirdTermsWithNoSecondTerm() {
    // Illustrative dates
    var schedule = schedule("PEDL", 143, version(1, LocalDate.of(2004, Month.JULY, 1),
        term("1", LocalDate.of(2010, Month.JULY, 1)),
        term("3", LocalDate.of(2034, Month.JULY, 1)),
        expiry(LocalDate.of(2034, Month.JULY, 1), null)
    ));

    assertThat(mapper.map(schedule, EVENT_TYPES).activeVersion().terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate)
        .containsExactly(
            tuple(1, LocalDate.of(2004, Month.JULY, 1), LocalDate.of(2010, Month.JUNE, 30)),
            tuple(3, LocalDate.of(2010, Month.JULY, 1), LocalDate.of(2034, Month.JUNE, 30))
        );
  }

  @Test
  void map_patternG_secondTermOnlyWithALaterExpiry() {
    var schedule = schedule("PEDL", 141, version(1, LocalDate.of(2004, Month.OCTOBER, 1),
        term("2", LocalDate.of(2013, Month.OCTOBER, 1)),
        expiry(LocalDate.of(2031, Month.OCTOBER, 1), null)
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.activeVersion().terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate, MappedTerm::duration)
        .containsExactly(tuple(2, LocalDate.of(2004, Month.OCTOBER, 1), LocalDate.of(2013, Month.SEPTEMBER, 30), years(9)));
    assertThat(mapped.notes().noteCountsByReason())
        .containsExactly(Map.entry(PearsScheduleMapper.EXPIRY_NOT_ON_FINAL_TERM_END, 1));
  }

  @Test
  void map_noExpiryEvent_isMigratedWithoutAnExpiryAndNoted() {
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        term("1", LocalDate.of(2018, Month.DECEMBER, 1))
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.activeVersion().expiry()).isNull();
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(Map.entry(PearsScheduleMapper.NO_EXPIRY, 1));
  }

  @Test
  void map_p1161_noCurrentVersion_makesTheLatestActive() {
    // Two versions, both end-dated, as P1161 has; the dates are illustrative
    var schedule = new PearsSchedule(new PearsLicenceReference("P", 1161), List.of(
        version(1, LocalDateTime.of(2006, Month.JANUARY, 1, 9, 0), LocalDateTime.of(2011, Month.JANUARY, 1, 9, 0),
            LocalDate.of(2005, Month.APRIL, 1), term("1", LocalDate.of(2009, Month.APRIL, 1))),
        version(2, LocalDateTime.of(2011, Month.JANUARY, 1, 9, 0), LocalDateTime.of(2011, Month.OCTOBER, 14, 14, 13),
            LocalDate.of(2005, Month.APRIL, 1), term("1", LocalDate.of(2010, Month.APRIL, 1)))
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.versions())
        .extracting(MappedScheduleVersion::plsdId, MappedScheduleVersion::status)
        .containsExactly(tuple(1L, LicenceScheduleDetailStatus.REPLACED), tuple(2L, LicenceScheduleDetailStatus.ACTIVE));
    assertThat(mapped.notes().noteCountsByReason())
        .containsEntry(PearsScheduleMapper.NO_CURRENT_VERSION, 1)
        .containsEntry(PearsScheduleMapper.NO_EXPIRY, 2);
  }

  @Test
  void map_everyVersionIsMappedOnItsOwn_withTheCurrentOneActive() {
    var schedule = new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(
        version(1, LocalDateTime.of(2014, Month.DECEMBER, 1, 9, 0), LocalDateTime.of(2018, Month.MAY, 2, 10, 0),
            LocalDate.of(2014, Month.DECEMBER, 1), term("1", LocalDate.of(2018, Month.DECEMBER, 1))),
        version(2, LocalDateTime.of(2018, Month.MAY, 2, 10, 0), null,
            LocalDate.of(2014, Month.DECEMBER, 1), term("1", LocalDate.of(2019, Month.DECEMBER, 1)))
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.versions())
        .extracting(MappedScheduleVersion::status, version -> version.finalTerm().boundary())
        .containsExactly(
            tuple(LicenceScheduleDetailStatus.REPLACED, LocalDate.of(2018, Month.DECEMBER, 1)),
            tuple(LicenceScheduleDetailStatus.ACTIVE, LocalDate.of(2019, Month.DECEMBER, 1))
        );
  }

  @Test
  void map_versionWithNoStartDate_borrowsTheNextVersionsAndWorksItsDatesOutFromTheOffsets() {
    var schedule = new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(
        version(1, LocalDateTime.of(2014, Month.NOVEMBER, 20, 9, 0), LocalDateTime.of(2014, Month.DECEMBER, 1, 9, 0),
            null, termAtOffset("1", "4", "0", "0", LocalDate.of(1904, Month.JANUARY, 1))),
        version(2, LocalDateTime.of(2014, Month.DECEMBER, 1, 9, 0), null,
            LocalDate.of(2014, Month.DECEMBER, 1), term("1", LocalDate.of(2018, Month.DECEMBER, 1)))
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    var firstVersion = mapped.versions().getFirst();
    assertThat(firstVersion.startDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 1));
    assertThat(firstVersion.startDateBorrowed()).isTrue();
    assertThat(firstVersion.datesFromOffsets()).isTrue();
    assertThat(firstVersion.finalTerm().boundary()).isEqualTo(LocalDate.of(2018, Month.DECEMBER, 1));
    assertThat(mapped.versions().getLast().startDateBorrowed()).isFalse();
    assertThat(mapped.versions().getLast().datesFromOffsets()).isFalse();
    assertThat(mapped.notes().noteCountsByReason()).containsEntry(PearsScheduleMapper.START_DATE_BORROWED, 1);
  }

  @Test
  void map_versionWhoseStoredDatesAreOnOrBeforeItsStartDate_worksThemOutFromTheOffsets() {
    // As 1,113 historical versions on live (data survey S19): dates from PEARS's 1900 base, offsets right
    var schedule = schedule("P", 1741, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        termAtOffset("1", "4", "0", "0", LocalDate.of(1904, Month.JANUARY, 1)),
        termAtOffset("2", "14", "0", "0", LocalDate.of(1914, Month.JANUARY, 1)),
        expiryAtOffset("14", LocalDate.of(1914, Month.JANUARY, 1))
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    var version = mapped.activeVersion();
    assertThat(version.startDateBorrowed()).isFalse();
    assertThat(version.datesFromOffsets()).isTrue();
    assertThat(version.terms())
        .extracting(MappedTerm::termNo, MappedTerm::startDate, MappedTerm::endDate)
        .containsExactly(
            tuple(1, LocalDate.of(2014, Month.DECEMBER, 1), LocalDate.of(2018, Month.NOVEMBER, 30)),
            tuple(2, LocalDate.of(2018, Month.DECEMBER, 1), LocalDate.of(2028, Month.NOVEMBER, 30))
        );
    assertThat(version.expiry()).isNotNull();
    assertThat(version.expiry().pearsDate()).isEqualTo(LocalDate.of(2028, Month.DECEMBER, 1));
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(Map.entry(PearsScheduleMapper.STALE_DATES, 1));
  }

  @Test
  void map_historicalVersionThatCannotBeMapped_isLeftOutAndListed() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);
    var schedule = new PearsSchedule(new PearsLicenceReference("PL", 178), List.of(
        version(1, LocalDateTime.of(2014, Month.DECEMBER, 1, 9, 0), LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0),
            startDate, term(null, startDate), event(null, null)),
        version(2, LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0), null,
            startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1)))
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.versions())
        .extracting(MappedScheduleVersion::plsdId, MappedScheduleVersion::status)
        .containsExactly(tuple(2L, LicenceScheduleDetailStatus.ACTIVE));
    assertThat(mapped.leftOutVersions())
        .containsExactly(new MappedSchedule.LeftOutVersion(1, PearsScheduleMappingException.NO_TERMS));
    // The left-out version's own notes (its unnumbered term and untyped event) aren't counted
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(
        Map.entry(PearsScheduleMapper.HISTORICAL_VERSION_LEFT_OUT, 1),
        Map.entry(PearsScheduleMapper.NO_EXPIRY, 1)
    );
  }

  @Test
  void map_activeVersionThatCannotBeMapped_isNotMigratedEvenWhenItsHistoryMaps() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);
    var schedule = new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(
        version(1, LocalDateTime.of(2014, Month.DECEMBER, 1, 9, 0), LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0),
            startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1))),
        version(2, LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0), null,
            startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1)), term("1", LocalDate.of(2019, Month.DECEMBER, 1)))
    ));

    assertNotMigrated(schedule, "Term 1 appears more than once (version 2)");
  }

  @Test
  void map_latestVersionWithNoStartDate_borrowsTheEarliestVersions() {
    var schedule = new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(
        version(1, LocalDateTime.of(2014, Month.DECEMBER, 1, 9, 0), LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0),
            LocalDate.of(2014, Month.DECEMBER, 1), term("1", LocalDate.of(2018, Month.DECEMBER, 1))),
        version(2, LocalDateTime.of(2015, Month.JANUARY, 1, 9, 0), null,
            null, termAtOffset("1", "4", "0", "0", null))
    ));

    var lastVersion = mapper.map(schedule, EVENT_TYPES).versions().getLast();

    assertThat(lastVersion.startDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 1));
    assertThat(lastVersion.startDateBorrowed()).isTrue();
  }

  @Test
  void map_noVersionWithAStartDate_isNotMigrated() {
    var schedule = schedule("P", 2244, version(1, null, termAtOffset("1", "4", "0", "0", null)));

    assertThatThrownBy(() -> mapper.map(schedule, EVENT_TYPES))
        .isInstanceOfSatisfying(PearsScheduleMappingException.class,
            e -> assertThat(e.reason()).isEqualTo("No schedule start date in any PEARS version"));
  }

  @Test
  void map_eventWithNoDate_worksItOutFromItsOffsetTheWayPearsDoes() {
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2015, Month.JANUARY, 30),
        termAtOffset("1", "", "1", "1", null)
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.activeVersion().finalTerm().boundary()).isEqualTo(LocalDate.of(2015, Month.FEBRUARY, 28));
    assertThat(mapped.notes().noteCountsByReason()).containsEntry(PearsScheduleMapper.EVENT_DATE_FROM_OFFSET, 1);
  }

  @Test
  void map_termWithNeitherADateNorAnOffset_isNotMigrated() {
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        termAtOffset("1", null, " ", null, null)
    ));

    assertThatThrownBy(() -> mapper.map(schedule, EVENT_TYPES))
        .isInstanceOfSatisfying(PearsScheduleMappingException.class,
            e -> assertThat(e.reason()).isEqualTo("Term 1 has no date (version 1)"));
  }

  @Test
  void map_untypedEvents_areIgnoredAndCountedByWhetherTheyHoldData() {
    var rateWithNoType = new PearsScheduleEvent(nextEventIndex++, " ", null, null, "17", "0", "0",
        LocalDate.of(2032, Month.DECEMBER, 1), "YEARLY", null, null, "7500", null, null, null, null, null);
    var schedule = schedule("P", 2582, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event(null, LocalDate.of(1900, Month.JANUARY, 1)),
        event("", null),
        rateWithNoType,
        expiry(LocalDate.of(2018, Month.DECEMBER, 1), null)
    ));

    var mapped = mapper.map(schedule, EVENT_TYPES);

    assertThat(mapped.notes().noteCountsByReason()).containsExactly(
        Map.entry(PearsScheduleMapper.UNTYPED_EVENT_EMPTY, 2),
        Map.entry(PearsScheduleMapper.UNTYPED_EVENT_WITH_DATA, 1)
    );
  }

  @Test
  void map_userDefinedEventType_isAccepted() {
    var eventTypes = new ArrayList<>(EVENT_TYPES);
    eventTypes.add(new PearsScheduleEventType("XX", "New activity", 11));
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("XX", LocalDate.of(2016, Month.DECEMBER, 1))
    ));

    assertThat(mapper.map(schedule, eventTypes).activeVersion().terms()).hasSize(1);
  }

  @Test
  void map_eventTypeNotInTheCatalogue_isNotMigrated() {
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("XX", LocalDate.of(2016, Month.DECEMBER, 1))
    ));

    assertNotMigrated(schedule, "Event type 'XX' is not known to the migration (version 1)");
  }

  @Test
  void map_catalogueTypeWithANegativeDisplaySequenceTheMigrationDoesNotMap_isNotMigrated() {
    var eventTypes = new ArrayList<>(EVENT_TYPES);
    eventTypes.add(new PearsScheduleEventType("XX", "Hidden", -5));
    var schedule = schedule("P", 2244, version(1, LocalDate.of(2014, Month.DECEMBER, 1),
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("XX", LocalDate.of(2016, Month.DECEMBER, 1))
    ));

    assertThatThrownBy(() -> mapper.map(schedule, eventTypes))
        .isInstanceOf(PearsScheduleMappingException.class)
        .hasMessage("P2244: Event type 'XX' is not known to the migration (version 1)");
  }

  @Test
  void map_termDataErrors_areNotMigrated() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);

    assertNotMigrated(
        schedule("P", 1, version(1, startDate,
            term("1", LocalDate.of(2018, Month.DECEMBER, 1)), term("1", LocalDate.of(2019, Month.DECEMBER, 1)))),
        "Term 1 appears more than once (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, term("5", LocalDate.of(2018, Month.DECEMBER, 1)))),
        "Term number '5' is not 1 to 4 (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, term("x", LocalDate.of(2018, Month.DECEMBER, 1)))),
        "Term number 'x' is not 1 to 4 (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate,
            term("1", LocalDate.of(2018, Month.DECEMBER, 1)), term("2", LocalDate.of(2017, Month.DECEMBER, 1)))),
        "Term 2 ends on 2017-12-01, on or before it starts on 2018-12-01 (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, termAtOffset("1", "0", "0", "0", startDate))),
        "Term 1 ends on 2014-12-01, on or before it starts on 2014-12-01 (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, term("1", null))),
        "Term 1 has no date (version 1)");
  }

  @Test
  void map_phaseDataErrors_areNotMigrated() {
    var startDate = LocalDate.of(2020, Month.DECEMBER, 1);
    var initialTermEnd = term("1", LocalDate.of(2024, Month.DECEMBER, 1));

    assertNotMigrated(
        schedule("P", 1, version(1, startDate, initialTermEnd, phase("D", LocalDate.of(2023, Month.DECEMBER, 1)))),
        "Phase 'D' is not A, B or C (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, initialTermEnd, phase(null, LocalDate.of(2023, Month.DECEMBER, 1)))),
        "Phase '' is not A, B or C (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, initialTermEnd,
            phase("A", LocalDate.of(2023, Month.DECEMBER, 1)), phase("A", LocalDate.of(2024, Month.DECEMBER, 1)))),
        "Phase A appears more than once (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, initialTermEnd,
            phase("A", LocalDate.of(2023, Month.DECEMBER, 1)), phase("B", LocalDate.of(2022, Month.DECEMBER, 1)))),
        "Phase B ends on 2022-12-01, on or before it starts on 2023-12-01 (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, initialTermEnd, phase("A", LocalDate.of(2023, Month.DECEMBER, 1)))),
        "The last phase ends on 2023-12-01, not with the initial term on 2024-12-01 (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate,
            term("2", LocalDate.of(2024, Month.DECEMBER, 1)), phase("A", LocalDate.of(2024, Month.DECEMBER, 1)))),
        "Phases with no initial term (version 1)");
  }

  @Test
  void map_expiryDataErrors_areNotMigrated() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);

    assertNotMigrated(
        schedule("P", 1, version(1, startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
            expiry(LocalDate.of(2018, Month.DECEMBER, 1), null), expiry(LocalDate.of(2019, Month.DECEMBER, 1), null))),
        "More than one expiry event (version 1)");
    assertNotMigrated(
        schedule("P", 1, version(1, startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
            term("2", LocalDate.of(2028, Month.DECEMBER, 1)), expiry(LocalDate.of(2018, Month.DECEMBER, 1), null))),
        "The expiry on 2018-12-01 is on or before the final term starts on 2018-12-01 (version 1)");
  }

  @Test
  void map_moreThanOneCurrentVersion_isNotMigrated() {
    var startDate = LocalDate.of(2014, Month.DECEMBER, 1);
    var schedule = new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(
        version(1, SAVED, null, startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1))),
        version(2, SAVED, null, startDate, term("1", LocalDate.of(2018, Month.DECEMBER, 1)))
    ));

    assertNotMigrated(schedule, "More than one current schedule version in PEARS");
  }

  @Test
  void map_noVersions_isNotMigrated() {
    assertNotMigrated(new PearsSchedule(new PearsLicenceReference("P", 2244), List.of()), "No schedule versions in PEARS");
  }

  private void assertNotMigrated(PearsSchedule schedule, String reason) {
    assertThatThrownBy(() -> mapper.map(schedule, EVENT_TYPES))
        .isInstanceOfSatisfying(PearsScheduleMappingException.class, e -> assertThat(e.reason()).isEqualTo(reason));
  }

  private static PearsSchedule schedule(String licenceType, int licenceNo, PearsScheduleVersion version) {
    return new PearsSchedule(new PearsLicenceReference(licenceType, licenceNo), List.of(version));
  }

  /**
   * The licence's only version, current.
   */
  private static PearsScheduleVersion version(long plsdId, LocalDate startDate, PearsScheduleEvent... events) {
    return version(plsdId, SAVED, null, startDate, events);
  }

  private static PearsScheduleVersion version(
      long plsdId,
      LocalDateTime saved,
      LocalDateTime replaced,
      LocalDate startDate,
      PearsScheduleEvent... events
  ) {
    return new PearsScheduleVersion(plsdId, saved, replaced, startDate, List.of(events));
  }

  private PearsScheduleEvent term(String termNo, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "T", termNo, null, null, null, null, date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent termAtOffset(String termNo, String years, String months, String days, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "T", termNo, null, years, months, days, date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent phase(String phase, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "P", null, phase, null, null, null, date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent expiryAtOffset(String years, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "E", null, null, years, "0", "0", date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent expiry(LocalDate date, String comments) {
    return new PearsScheduleEvent(nextEventIndex++, "E", null, null, null, null, null, date,
        null, null, null, null, null, comments, null, null, null);
  }

  private PearsScheduleEvent yearlyRate(String rate, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "R", null, null, null, null, null, date,
        "YEARLY", null, null, rate, null, null, null, null, null);
  }

  private PearsScheduleEvent event(String eventMnem, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, eventMnem, null, null, null, null, null, date,
        null, null, null, null, null, null, null, null, null);
  }

  private static ThreeFieldDuration years(int years) {
    return new ThreeFieldDuration(years, 0, 0);
  }
}
