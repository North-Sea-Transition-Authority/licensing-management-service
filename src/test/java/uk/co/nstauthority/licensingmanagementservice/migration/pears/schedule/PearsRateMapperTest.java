package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateDefinitionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.RateRelativeDateOption;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Maps fixture schedules' rates, shaped like the licences the plan and data survey (S12, S12b, S12c) use as examples.
 * Where the survey gives a licence's rates and dates they are used as they are; anything else is illustrative and says
 * so.
 */
@ExtendWith(MockitoExtension.class)
class PearsRateMapperTest {

  private static final List<PearsScheduleEventType> EVENT_TYPES = List.of(
      new PearsScheduleEventType("P", "Phase", -4),
      new PearsScheduleEventType("T", "Term", -3),
      new PearsScheduleEventType("R", "Rate", -2),
      new PearsScheduleEventType("E", "Licence Expiry Date", 1)
  );

  private static final RateDefinitionOption TERM = RateDefinitionOption.TERM;
  private static final RateDefinitionOption PHASE = RateDefinitionOption.PHASE;
  private static final RateDefinitionOption CUSTOM = RateDefinitionOption.CUSTOM_PERIOD;
  private static final RateRelativeDateOption ON_START = RateRelativeDateOption.ON_START_DATE;
  private static final RateRelativeDateOption RELATIVE = RateRelativeDateOption.RELATIVE_TO_START_DATE;

  private final PearsScheduleMapper mapper = new PearsScheduleMapper();

  private int nextEventIndex = 1;

  @Test
  void map_p2522_oneRatePerAnniversaryThenAYearlyRateRelativeToTheThirdTerm() {
    // P2522's start, phases, terms and yearly 7500 from 01-Dec-2032 (plan, Open question 7); the rates before it are
    // illustrative, in the "one non-recurring rate per anniversary" shape every P schedule has (survey S12)
    var mapped = map(LocalDate.of(2020, Month.DECEMBER, 1),
        phase("A", LocalDate.of(2023, Month.DECEMBER, 1)),
        phase("C", LocalDate.of(2024, Month.DECEMBER, 1)),
        term("1", LocalDate.of(2024, Month.DECEMBER, 1)),
        term("2", LocalDate.of(2027, Month.DECEMBER, 1)),
        term("3", LocalDate.of(2045, Month.DECEMBER, 1)),
        rate("150", LocalDate.of(2020, Month.DECEMBER, 1)),
        rate("150", LocalDate.of(2021, Month.DECEMBER, 1)),
        rate("150", LocalDate.of(2022, Month.DECEMBER, 1)),
        rate("300", LocalDate.of(2023, Month.DECEMBER, 1)),
        rate("600", LocalDate.of(2024, Month.DECEMBER, 1)),
        rate("600", LocalDate.of(2025, Month.DECEMBER, 1)),
        rate("600", LocalDate.of(2026, Month.DECEMBER, 1)),
        countedRate("3000", LocalDate.of(2027, Month.DECEMBER, 1), "4"),
        yearlyRate("7500", LocalDate.of(2032, Month.DECEMBER, 1)),
        expiry(LocalDate.of(2045, Month.DECEMBER, 1))
    );

    assertRates(mapped,
        tuple(LocalDate.of(2020, Month.DECEMBER, 1), new BigDecimal("150"), 1, PhaseType.PHASE_A, CUSTOM, ON_START, null),
        tuple(LocalDate.of(2021, Month.DECEMBER, 1), new BigDecimal("150"), 1, PhaseType.PHASE_A, CUSTOM, RELATIVE,
            years(1)),
        tuple(LocalDate.of(2022, Month.DECEMBER, 1), new BigDecimal("150"), 1, PhaseType.PHASE_A, CUSTOM, RELATIVE,
            years(2)),
        // The only rate in phase C, starting and ending with it
        tuple(LocalDate.of(2023, Month.DECEMBER, 1), new BigDecimal("300"), 1, PhaseType.PHASE_C, PHASE, null, null),
        tuple(LocalDate.of(2024, Month.DECEMBER, 1), new BigDecimal("600"), 2, null, CUSTOM, ON_START, null),
        tuple(LocalDate.of(2025, Month.DECEMBER, 1), new BigDecimal("600"), 2, null, CUSTOM, RELATIVE, years(1)),
        tuple(LocalDate.of(2026, Month.DECEMBER, 1), new BigDecimal("600"), 2, null, CUSTOM, RELATIVE, years(2)),
        tuple(LocalDate.of(2027, Month.DECEMBER, 1), new BigDecimal("3000"), 3, null, CUSTOM, ON_START, null),
        // Plan: "P2522 gets one LMS rate (Third term + 5y)"
        tuple(LocalDate.of(2032, Month.DECEMBER, 1), new BigDecimal("7500"), 3, null, CUSTOM, RELATIVE, years(5))
    );
    assertThat(mapped.notes().noteCountsByReason()).isEmpty();
  }

  @Test
  void map_yearlyRateCrossingPhaseAndTermBoundaries_isOneRatePerPhaseAndTermOnItsFirstAnniversaryThere() {
    // Illustrative dates: phase A ends half way through a year, so phase B's first anniversary is 6 months in
    var mapped = map(LocalDate.of(2020, Month.JANUARY, 1),
        phase("A", LocalDate.of(2022, Month.JULY, 1)),
        phase("B", LocalDate.of(2024, Month.JANUARY, 1)),
        term("1", LocalDate.of(2024, Month.JANUARY, 1)),
        term("2", LocalDate.of(2027, Month.JANUARY, 1)),
        term("3", LocalDate.of(2030, Month.JANUARY, 1)),
        yearlyRate("100", LocalDate.of(2020, Month.JANUARY, 1))
    );

    assertRates(mapped,
        // Runs on past phase A's end, until phase B's rate starts
        tuple(LocalDate.of(2020, Month.JANUARY, 1), new BigDecimal("100"), 1, PhaseType.PHASE_A, CUSTOM, ON_START, null),
        tuple(LocalDate.of(2023, Month.JANUARY, 1), new BigDecimal("100"), 1, PhaseType.PHASE_B, CUSTOM, RELATIVE,
            new ThreeFieldDuration(0, 6, 0)),
        tuple(LocalDate.of(2024, Month.JANUARY, 1), new BigDecimal("100"), 2, null, TERM, null, null),
        tuple(LocalDate.of(2027, Month.JANUARY, 1), new BigDecimal("100"), 3, null, TERM, null, null)
    );
    assertThat(mapped.notes().noteCountsByReason()).isEmpty();
  }

  @Test
  void map_p516_yearsWithNoRate_carryThePreviousRateOnAndAreNoted() {
    // P516's rates (survey S12b); its terms are illustrative
    var mapped = map(LocalDate.of(1985, Month.JUNE, 14),
        term("1", LocalDate.of(1991, Month.JUNE, 14)),
        term("2", LocalDate.of(2003, Month.JUNE, 14)),
        rate("330", LocalDate.of(1985, Month.JUNE, 14)),
        yearlyRate("400", LocalDate.of(1991, Month.JUNE, 14))
    );

    assertRates(mapped,
        tuple(LocalDate.of(1985, Month.JUNE, 14), new BigDecimal("330"), 1, null, TERM, null, null),
        tuple(LocalDate.of(1991, Month.JUNE, 14), new BigDecimal("400"), 2, null, TERM, null, null)
    );
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(Map.entry(PearsRateMapper.RATE_GAP_CARRIED_ON, 1));
  }

  @Test
  void map_gapThatRunsIntoTheNextTerm_givesThatTermItsOwnRateOnTheAnniversary() {
    // Illustrative: 100 doesn't recur and the next rate is 5 years later, past the end of the initial term
    var mapped = map(LocalDate.of(2020, Month.JANUARY, 1),
        term("1", LocalDate.of(2022, Month.JULY, 1)),
        term("2", LocalDate.of(2030, Month.JULY, 1)),
        rate("100", LocalDate.of(2020, Month.JANUARY, 1)),
        yearlyRate("200", LocalDate.of(2025, Month.JANUARY, 1))
    );

    assertRates(mapped,
        tuple(LocalDate.of(2020, Month.JANUARY, 1), new BigDecimal("100"), 1, null, CUSTOM, ON_START, null),
        tuple(LocalDate.of(2023, Month.JANUARY, 1), new BigDecimal("100"), 2, null, CUSTOM, RELATIVE,
            new ThreeFieldDuration(0, 6, 0)),
        tuple(LocalDate.of(2025, Month.JANUARY, 1), new BigDecimal("200"), 2, null, CUSTOM, RELATIVE,
            new ThreeFieldDuration(2, 6, 0))
    );
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(Map.entry(PearsRateMapper.RATE_GAP_CARRIED_ON, 1));
  }

  @Test
  void map_pl116_countedRatesThatRunUpToTheNextRate_needNothingMore() {
    // PL116's rates (survey S12b); its terms are illustrative
    var mapped = map(LocalDate.of(1969, Month.AUGUST, 1),
        term("1", LocalDate.of(1975, Month.AUGUST, 1)),
        term("2", LocalDate.of(2005, Month.AUGUST, 1)),
        countedRate(".5", LocalDate.of(1969, Month.AUGUST, 1), "2"),
        countedRate("1", LocalDate.of(1972, Month.AUGUST, 1), "2"),
        yearlyRate("20", LocalDate.of(1975, Month.AUGUST, 1))
    );

    assertRates(mapped,
        tuple(LocalDate.of(1969, Month.AUGUST, 1), new BigDecimal("0.50"), 1, null, CUSTOM, ON_START, null),
        tuple(LocalDate.of(1972, Month.AUGUST, 1), new BigDecimal("1"), 1, null, CUSTOM, RELATIVE, years(3)),
        tuple(LocalDate.of(1975, Month.AUGUST, 1), new BigDecimal("20"), 2, null, TERM, null, null)
    );
    assertThat(mapped.notes().noteCountsByReason()).isEmpty();
  }

  @Test
  void map_al10_countedRateThatRunsIntoTheNextRate_stopsWhenItStartsAndIsNoted() {
    // AL10's rates (survey S12b, S12c): 25 seven times from 1995, the seventh on the day 50 starts; its terms are
    // illustrative
    var mapped = map(LocalDate.of(1995, Month.JUNE, 26),
        term("1", LocalDate.of(2004, Month.JUNE, 26)),
        term("2", LocalDate.of(2020, Month.JUNE, 26)),
        countedRate("25", LocalDate.of(1995, Month.JUNE, 26), "6"),
        yearlyRate("50", LocalDate.of(2001, Month.JUNE, 26))
    );

    assertRates(mapped,
        tuple(LocalDate.of(1995, Month.JUNE, 26), new BigDecimal("25"), 1, null, CUSTOM, ON_START, null),
        tuple(LocalDate.of(2001, Month.JUNE, 26), new BigDecimal("50"), 1, null, CUSTOM, RELATIVE, years(6)),
        tuple(LocalDate.of(2004, Month.JUNE, 26), new BigDecimal("50"), 2, null, TERM, null, null)
    );
    assertThat(mapped.notes().noteCountsByReason())
        .containsExactly(Map.entry(PearsRateMapper.COUNTED_RATE_RUNS_INTO_NEXT, 1));
  }

  @Test
  void map_p2644_endlessYearlyRateFollowedByAnotherRate_stopsWhenItStartsAndIsNoted() {
    // P2644's last two rates and licence end (survey S12b); its start and term are illustrative
    var mapped = map(LocalDate.of(2024, Month.MARCH, 1),
        term("1", LocalDate.of(2052, Month.MARCH, 1)),
        yearlyRate("7500", LocalDate.of(2038, Month.MARCH, 1)),
        rate("150", LocalDate.of(2039, Month.MARCH, 1))
    );

    assertRates(mapped,
        tuple(LocalDate.of(2038, Month.MARCH, 1), new BigDecimal("7500"), 1, null, CUSTOM, RELATIVE, years(14)),
        tuple(LocalDate.of(2039, Month.MARCH, 1), new BigDecimal("150"), 1, null, CUSTOM, RELATIVE, years(15))
    );
    // 150 doesn't recur, so the 12 years to the licence end have no rate in PEARS
    assertThat(mapped.notes().noteCountsByReason()).isEqualTo(Map.of(
        PearsRateMapper.INDEFINITE_RATE_NOT_LAST, 1,
        PearsRateMapper.RATE_GAP_CARRIED_ON, 1
    ));
  }

  @Test
  void map_yearlyRateAtAnOffset_recursWhereItsOffsetPlacesItAsPearsDoes() {
    // Illustrative: PEARS places the rate at 0y 1m 27d, which is 28-Feb-2015 and, a year on, 28-Feb-2016. A year after
    // 28-Feb-2015 by ADD_MONTHS alone would be 29-Feb-2016, as 28-Feb-2015 is a month end
    var mapped = map(LocalDate.of(2015, Month.JANUARY, 1),
        term("1", LocalDate.of(2016, Month.JANUARY, 1)),
        term("2", LocalDate.of(2030, Month.JANUARY, 1)),
        rateAtOffset("100", "0", "1", "27", LocalDate.of(2015, Month.FEBRUARY, 28))
    );

    var oneMonth27Days = new ThreeFieldDuration(0, 1, 27);
    assertRates(mapped,
        tuple(LocalDate.of(2015, Month.FEBRUARY, 28), new BigDecimal("100"), 1, null, CUSTOM, RELATIVE, oneMonth27Days),
        tuple(LocalDate.of(2016, Month.FEBRUARY, 28), new BigDecimal("100"), 2, null, CUSTOM, RELATIVE, oneMonth27Days)
    );
  }

  @Test
  void map_rateWhoseOffsetLmsWouldPlaceElsewhere_landsOnThePearsDate() {
    // P1609's start (survey S14b); the rate's offset is illustrative, of the same kind as P1609's T(1). PEARS places
    // 5y 10m 16d at 31-Dec-2014 (12-Feb-2009 + 16 days is a month end); copied to LMS it would land on 28-Dec-2014
    var mapped = map(LocalDate.of(2009, Month.FEBRUARY, 12),
        term("1", LocalDate.of(2016, Month.DECEMBER, 31)),
        rateAtOffset("150", "5", "10", "16", LocalDate.of(2014, Month.DECEMBER, 31))
    );

    var rate = mapped.activeVersion().rates().getFirst();
    assertThat(rate.startDate()).isEqualTo(LocalDate.of(2014, Month.DECEMBER, 31));
    assertThat(rate.relativeDuration()).isEqualTo(new ThreeFieldDuration(5, 10, 19));
    assertThat(LocalDate.of(2009, Month.FEBRUARY, 12).plusYears(5).plusMonths(10).plusDays(19))
        .isEqualTo(rate.startDate());
  }

  @Test
  void map_yearlyRateWithNoOffset_recursAYearApartFromItsDate() {
    var mapped = map(LocalDate.of(2015, Month.JANUARY, 1),
        term("1", LocalDate.of(2016, Month.JANUARY, 1)),
        term("2", LocalDate.of(2030, Month.JANUARY, 1)),
        yearlyRate("100", LocalDate.of(2015, Month.FEBRUARY, 28))
    );

    assertThat(mapped.activeVersion().rates())
        .extracting(MappedRate::startDate)
        .containsExactly(LocalDate.of(2015, Month.FEBRUARY, 28), LocalDate.of(2016, Month.FEBRUARY, 29));
  }

  @Test
  void map_rateValues_areKeptToPenceAndAnyThatCannotBeUsedAreLeftOutAndNoted() {
    var mapped = map(LocalDate.of(2020, Month.JANUARY, 1),
        term("1", LocalDate.of(2030, Month.JANUARY, 1)),
        rate(" 7500 ", LocalDate.of(2020, Month.JANUARY, 1)),
        rate("12.5", LocalDate.of(2021, Month.JANUARY, 1)),
        rate("12.345", LocalDate.of(2022, Month.JANUARY, 1)),
        yearlyRate("0", LocalDate.of(2023, Month.JANUARY, 1)),
        rate("", LocalDate.of(2024, Month.JANUARY, 1)),
        rate(null, LocalDate.of(2025, Month.JANUARY, 1)),
        rate("£15", LocalDate.of(2026, Month.JANUARY, 1)),
        rate("-5", LocalDate.of(2027, Month.JANUARY, 1)),
        rate("1E3", LocalDate.of(2028, Month.JANUARY, 1))
    );

    assertThat(mapped.activeVersion().rates())
        .extracting(MappedRate::rentalRate)
        .containsExactly(new BigDecimal("7500"), new BigDecimal("12.50"), new BigDecimal("12.35"), new BigDecimal("0"));
    assertThat(mapped.notes().noteCountsByReason()).isEqualTo(Map.of(
        PearsRateMapper.RATE_ROUNDED, 1,
        PearsRateMapper.RATE_NOT_A_VALUE, 5
    ));
  }

  @Test
  void map_ratesOutsideTheTerms_areLeftOutAndNoted() {
    var mapped = map(LocalDate.of(2020, Month.JANUARY, 1),
        term("1", LocalDate.of(2030, Month.JANUARY, 1)),
        yearlyRate("100", LocalDate.of(2019, Month.JANUARY, 1)),
        yearlyRate("200", LocalDate.of(2020, Month.JANUARY, 1)),
        yearlyRate("300", LocalDate.of(2030, Month.JANUARY, 1)),
        expiry(LocalDate.of(2030, Month.JANUARY, 1))
    );

    assertRates(mapped,
        tuple(LocalDate.of(2020, Month.JANUARY, 1), new BigDecimal("200"), 1, null, TERM, null, null)
    );
    assertThat(mapped.notes().noteCountsByReason()).containsExactly(Map.entry(PearsRateMapper.RATE_OUTSIDE_TERMS, 2));
  }

  @Test
  void map_rateDataErrors_areNotMigrated() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 1);
    var termEnd = LocalDate.of(2030, Month.JANUARY, 1);
    var rateDate = LocalDate.of(2021, Month.JANUARY, 1);

    assertNotMigrated(schedule(startDate, term("1", termEnd), rate("100", rateDate), yearlyRate("200", rateDate)),
        "Two rates start on 2021-01-01 (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("100", null, rateDate, "MONTHLY", null)),
        "Rate recurrence 'MONTHLY' is not YEARLY (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("100", null, rateDate, "YEARLY", "x")),
        "Rate recurrence count 'x' is not a whole number (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("100", null, null, null, null)),
        "A rate of 100 has no date (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("", null, null, null, null)),
        "A rate has no date (version 1)");
  }

  @Test
  void map_rateDataErrors_onRatesThatWouldBeLeftOut_areStillNotMigrated() {
    var startDate = LocalDate.of(2020, Month.JANUARY, 1);
    var termEnd = LocalDate.of(2030, Month.JANUARY, 1);
    var afterTheTerms = LocalDate.of(2031, Month.JANUARY, 1);
    var rateDate = LocalDate.of(2021, Month.JANUARY, 1);

    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("100", null, afterTheTerms, "MONTHLY", null)),
        "Rate recurrence 'MONTHLY' is not YEARLY (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("100", null, afterTheTerms, "YEARLY", "x")),
        "Rate recurrence count 'x' is not a whole number (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rateEvent("x", null, rateDate, "MONTHLY", null)),
        "Rate recurrence 'MONTHLY' is not YEARLY (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rate("£15", rateDate), yearlyRate("200", rateDate)),
        "Two rates start on 2021-01-01 (version 1)");
    assertNotMigrated(schedule(startDate, term("1", termEnd), rate("100", afterTheTerms), yearlyRate("200", afterTheTerms)),
        "Two rates start on 2031-01-01 (version 1)");
  }

  /**
   * Maps a single-version schedule. When the events have no expiry, one is added on the final term's end, as almost
   * every schedule has, so the notes are only the rates'.
   */
  private MappedSchedule map(LocalDate startDate, PearsScheduleEvent... events) {
    var allEvents = new ArrayList<>(List.of(events));

    if (allEvents.stream().noneMatch(event -> "E".equals(event.eventMnem()))) {
      allEvents.stream()
          .filter(event -> "T".equals(event.eventMnem()))
          .map(PearsScheduleEvent::eventDate)
          .max(Comparator.naturalOrder())
          .ifPresent(finalTermEnd -> allEvents.add(expiry(finalTermEnd)));
    }

    return mapper.map(schedule(startDate, allEvents.toArray(PearsScheduleEvent[]::new)), EVENT_TYPES);
  }

  private void assertNotMigrated(PearsSchedule schedule, String reason) {
    assertThatThrownBy(() -> mapper.map(schedule, EVENT_TYPES))
        .isInstanceOfSatisfying(PearsScheduleMappingException.class, e -> assertThat(e.reason()).isEqualTo(reason));
  }

  private static void assertRates(MappedSchedule mapped, Tuple... rates) {
    assertThat(mapped.activeVersion().rates())
        .extracting(
            MappedRate::startDate,
            MappedRate::rentalRate,
            MappedRate::termNo,
            MappedRate::phaseType,
            MappedRate::definitionOption,
            MappedRate::relativeDateOption,
            MappedRate::relativeDuration
        )
        .containsExactly(rates);
  }

  private static PearsSchedule schedule(LocalDate startDate, PearsScheduleEvent... events) {
    var version = new PearsScheduleVersion(1, LocalDateTime.of(2024, Month.JUNE, 3, 14, 30), null, startDate,
        List.of(events));
    return new PearsSchedule(new PearsLicenceReference("P", 1), List.of(version));
  }

  private PearsScheduleEvent term(String termNo, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "T", termNo, null, null, null, null, date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent phase(String phase, LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "P", null, phase, null, null, null, date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent expiry(LocalDate date) {
    return new PearsScheduleEvent(nextEventIndex++, "E", null, null, null, null, null, date,
        null, null, null, null, null, null, null, null, null);
  }

  private PearsScheduleEvent rate(String rate, LocalDate date) {
    return rateEvent(rate, null, date, null, null);
  }

  private PearsScheduleEvent yearlyRate(String rate, LocalDate date) {
    return rateEvent(rate, null, date, "YEARLY", null);
  }

  private PearsScheduleEvent countedRate(String rate, LocalDate date, String recurrances) {
    return rateEvent(rate, null, date, "YEARLY", recurrances);
  }

  private PearsScheduleEvent rateAtOffset(String rate, String years, String months, String days, LocalDate date) {
    return rateEvent(rate, new String[] {years, months, days}, date, "YEARLY", null);
  }

  private PearsScheduleEvent rateEvent(
      String rate,
      String[] offset,
      LocalDate date,
      String recurring,
      String recurrances
  ) {
    return new PearsScheduleEvent(nextEventIndex++, "R", null, null,
        offset != null ? offset[0] : null, offset != null ? offset[1] : null, offset != null ? offset[2] : null, date,
        recurring, recurrances, null, rate, null, null, null, null, null);
  }

  private static ThreeFieldDuration years(int years) {
    return new ThreeFieldDuration(years, 0, 0);
  }
}
