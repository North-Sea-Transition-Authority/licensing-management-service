package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.event;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.phase;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.term;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.MappedOtherEvent.Category;

/**
 * Maps fixture schedules' other events, shaped like the licences the plan and data survey (S13, S13c) use as examples.
 * Where a licence's events are known they are used as they are; anything else is illustrative and says so.
 */
@ExtendWith(MockitoExtension.class)
class PearsOtherEventMapperTest {

  /**
   * The PEARS event type catalogue on live (data survey S1), plus an illustrative user-defined type.
   */
  private static final List<PearsScheduleEventType> EVENT_TYPES = List.of(
      new PearsScheduleEventType("P", "Phase", -4),
      new PearsScheduleEventType("T", "Term", -3),
      new PearsScheduleEventType("R", "Rate", -2),
      new PearsScheduleEventType("MR", "Mandatory Relinquishment", -1),
      new PearsScheduleEventType("E", "Licence Expiry Date", 1),
      new PearsScheduleEventType("BM", "Bespoke Milestone", 6),
      new PearsScheduleEventType("DD", "Drill-or-drop", 7),
      new PearsScheduleEventType("2M", "Second Term with autosurrender", 8),
      new PearsScheduleEventType("2N", "2nd term with all acreage", 9),
      new PearsScheduleEventType("SS", " Seismic survey ", 20)
  );

  private static final LocalDate P2244_START = LocalDate.of(2014, Month.DECEMBER, 1);

  private final PearsScheduleMapper mapper = new PearsScheduleMapper();

  @Test
  void map_p2244_relinquishmentsAndBespokeMilestones_areAnchoredToTheTermTheyFallIn() {
    // P2244's terms, bespoke milestones and first relinquishment (plan, "Which term an event belongs to"); the later
    // relinquishment's date is illustrative
    var mapped = p2244(
        event("MR", LocalDate.of(2018, Month.DECEMBER, 1)).withComments("50% Relinquishment"),
        event("BM", LocalDate.of(2023, Month.NOVEMBER, 30)),
        event("BM", LocalDate.of(2024, Month.MARCH, 31)),
        event("MR", LocalDate.of(2028, Month.DECEMBER, 1))
            .withPercentage("0")
            .withComments("Everything outside FD areas")
    );

    assertThat(mapped.activeVersion().otherEvents())
        .extracting(
            MappedOtherEvent::category,
            MappedOtherEvent::description,
            MappedOtherEvent::defaultDescription,
            MappedOtherEvent::relinquishmentPercentage,
            MappedOtherEvent::date,
            MappedOtherEvent::termNo,
            MappedOtherEvent::relativeDuration
        )
        .containsExactly(
            // On the initial term's end, so it belongs to the initial term with the term's full length as its offset
            tuple(Category.MANDATORY_RELINQUISHMENT, "50% Relinquishment", false, null,
                LocalDate.of(2018, Month.DECEMBER, 1), 1, new ThreeFieldDuration(4, 0, 0)),
            tuple(Category.BESPOKE_MILESTONE, "Bespoke milestone", true, null,
                LocalDate.of(2023, Month.NOVEMBER, 30), 2, new ThreeFieldDuration(4, 11, 29)),
            tuple(Category.BESPOKE_MILESTONE, "Bespoke milestone", true, null,
                LocalDate.of(2024, Month.MARCH, 31), 2, new ThreeFieldDuration(5, 3, 30)),
            tuple(Category.MANDATORY_RELINQUISHMENT, "Everything outside FD areas", false, new BigDecimal("0"),
                LocalDate.of(2028, Month.DECEMBER, 1), 2, new ThreeFieldDuration(10, 0, 0))
        );
    assertThat(mapped.activeVersion().otherEvents())
        .extracting(MappedOtherEvent::otherCategoryName)
        .containsOnlyNulls();
    assertThat(mapped.notes().noteCountsByReason())
        .containsExactly(Map.entry(PearsOtherEventMapper.DEFAULT_DESCRIPTION, 2));
  }

  @Test
  void map_relinquishmentPercentages_followTheDecidedRules() {
    // The values on live (data survey S13, S13c), on illustrative dates
    var mapped = p2244(
        relinquishment(2015, "50"),
        relinquishment(2016, " 87.5 "),
        relinquishment(2017, "50%"),
        relinquishment(2018, "x"),
        relinquishment(2019, null).withComments("50% relinquishment"),
        relinquishment(2020, "Everything outside FD areas"),
        relinquishment(2021, "12.25"),
        relinquishment(2022, "150")
    );

    assertThat(mapped.activeVersion().otherEvents())
        .extracting(MappedOtherEvent::relinquishmentPercentage)
        .containsExactly(new BigDecimal("50"), new BigDecimal("87.5"), new BigDecimal("50"), null, null, null, null, null);
    assertThat(mapped.activeVersion().otherEvents().get(4).description()).isEqualTo("50% relinquishment");
    assertThat(mapped.notes().noteCountsByReason()).isEqualTo(Map.of(
        PearsOtherEventMapper.PERCENTAGE_PLACEHOLDER, 1,
        PearsOtherEventMapper.PERCENTAGE_NOT_MIGRATED, 3,
        PearsOtherEventMapper.DEFAULT_DESCRIPTION, 7
    ));
  }

  @Test
  void map_otherActivityTypes_areNamedByTheirPearsShortTitle() {
    // Illustrative dates
    var mapped = p2244(
        event("DD", LocalDate.of(2017, Month.JUNE, 1)),
        event("2M", LocalDate.of(2018, Month.DECEMBER, 1)).withComments("Relinquish 50% or surrender"),
        event("2N", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("SS", LocalDate.of(2020, Month.JANUARY, 1))
    );

    assertThat(mapped.activeVersion().otherEvents())
        .extracting(MappedOtherEvent::category, MappedOtherEvent::otherCategoryName, MappedOtherEvent::description)
        .containsExactly(
            tuple(Category.OTHER_ACTIVITY, "Drill-or-drop", "Drill-or-drop"),
            tuple(Category.OTHER_ACTIVITY, "Second Term with autosurrender", "Relinquish 50% or surrender"),
            tuple(Category.OTHER_ACTIVITY, "2nd term with all acreage", "2nd term with all acreage"),
            tuple(Category.OTHER_ACTIVITY, "Seismic survey", "Seismic survey")
        );
  }

  @Test
  void map_eventInAPhase_isAnchoredToTheInitialTerm() {
    // P2522's start, phases and initial term; the event is illustrative
    var mapped = map(LocalDate.of(2020, Month.DECEMBER, 1),
        phase("A", LocalDate.of(2023, Month.DECEMBER, 1)),
        phase("C", LocalDate.of(2024, Month.DECEMBER, 1)),
        term("1", LocalDate.of(2024, Month.DECEMBER, 1)),
        event("DD", LocalDate.of(2024, Month.JUNE, 1))
    );

    assertThat(mapped.activeVersion().otherEvents())
        .extracting(MappedOtherEvent::termNo, MappedOtherEvent::relativeDuration)
        .containsExactly(tuple(1, new ThreeFieldDuration(3, 6, 0)));
  }

  @Test
  void map_eventsOutsideTheTerms_areLeftOutBeforeTheStartAndMigratedAndNotedAfterTheEnd() {
    var mapped = p2244(
        event("BM", LocalDate.of(2014, Month.NOVEMBER, 30)).withComments("Before the start"),
        event("BM", LocalDate.of(2040, Month.DECEMBER, 1)).withComments("On the licence end"),
        event("BM", LocalDate.of(2041, Month.JUNE, 1)).withComments("After the licence end")
    );

    assertThat(mapped.activeVersion().otherEvents())
        .extracting(MappedOtherEvent::description, MappedOtherEvent::termNo, MappedOtherEvent::relativeDuration)
        .containsExactly(
            tuple("On the licence end", 3, new ThreeFieldDuration(12, 0, 0)),
            tuple("After the licence end", 3, new ThreeFieldDuration(12, 6, 0))
        );
    assertThat(mapped.notes().noteCountsByReason()).isEqualTo(Map.of(
        PearsOtherEventMapper.EVENT_BEFORE_START, 1,
        PearsOtherEventMapper.EVENT_AFTER_FINAL_TERM, 2
    ));
  }

  @Test
  void map_otherEventWithNoDate_isNotMigrated() {
    var schedule = schedule(P2244_START, List.of(
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("BM", null)
    ));

    assertThatThrownBy(() -> mapper.map(schedule, EVENT_TYPES))
        .isInstanceOfSatisfying(PearsScheduleMappingException.class,
            e -> assertThat(e.reason()).isEqualTo("A BM event has no date (version 1)"));
  }

  private MappedSchedule map(LocalDate startDate, PearsEventFixture... events) {
    return mapper.map(schedule(startDate, List.of(events)), EVENT_TYPES);
  }

  /**
   * Maps P2244's terms and expiry with the given events.
   */
  private MappedSchedule p2244(PearsEventFixture... events) {
    var allEvents = new ArrayList<>(List.of(
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)),
        term("3", LocalDate.of(2040, Month.DECEMBER, 1)),
        event("E", LocalDate.of(2040, Month.DECEMBER, 1))
    ));
    allEvents.addAll(List.of(events));
    return mapper.map(schedule(P2244_START, allEvents), EVENT_TYPES);
  }

  private static PearsEventFixture relinquishment(int year, String percentage) {
    return event("MR", LocalDate.of(year, Month.JANUARY, 1)).withPercentage(percentage);
  }

  private static PearsSchedule schedule(LocalDate startDate, List<PearsEventFixture> events) {
    var version = new PearsScheduleVersion(1, LocalDateTime.of(2024, Month.JUNE, 3, 14, 30), null, startDate,
        events.stream().map(PearsEventFixture::build).toList());
    return new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(version));
  }
}
