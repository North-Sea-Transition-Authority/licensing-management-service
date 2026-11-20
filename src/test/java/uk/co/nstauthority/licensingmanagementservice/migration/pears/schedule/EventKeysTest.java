package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.event;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.term;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

/**
 * Maps multi-version fixture schedules, shaped like P2244 (start 01-Dec-2014) with illustrative history, and checks
 * which events keep their key from one version to the next.
 */
@ExtendWith(MockitoExtension.class)
class EventKeysTest {

  private static final List<PearsScheduleEventType> EVENT_TYPES = List.of(
      new PearsScheduleEventType("T", "Term", -3),
      new PearsScheduleEventType("R", "Rate", -2),
      new PearsScheduleEventType("MR", "Mandatory Relinquishment", -1),
      new PearsScheduleEventType("E", "Licence Expiry Date", 1),
      new PearsScheduleEventType("BM", "Bespoke Milestone", 6),
      new PearsScheduleEventType("DD", "Drill-or-drop", 7)
  );

  private static final LocalDate START = LocalDate.of(2014, Month.DECEMBER, 1);

  private final PearsScheduleMapper mapper = new PearsScheduleMapper();

  @Test
  void map_eventsMatchedToThePreviousVersion_keepTheirKeys() {
    var first = version(1, 2014, false,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments("Initial term"),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0),
        event("E", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150").withComments("Initial rental"),
        event("MR", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments("50% Relinquishment"),
        event("BM", LocalDate.of(2023, Month.DECEMBER, 1)).withOffset(9, 0, 0)
    );
    var second = version(2, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments("Initial term"),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0).withComments("Second term extended"),
        term("3", LocalDate.of(2040, Month.DECEMBER, 1)).withOffset(26, 0, 0),
        event("E", LocalDate.of(2040, Month.DECEMBER, 1)).withOffset(26, 0, 0),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150").withComments("Initial rental"),
        // Moved six months, but its comment is unique, so it is the same event
        event("MR", LocalDate.of(2019, Month.JUNE, 1)).withOffset(4, 6, 0).withComments("50% Relinquishment"),
        event("BM", LocalDate.of(2023, Month.DECEMBER, 1)).withOffset(9, 0, 0),
        event("DD", LocalDate.of(2019, Month.DECEMBER, 1)).withOffset(5, 0, 0)
    );

    var mapped = map(first, second);
    var v1 = mapped.versions().get(0);
    var v2 = mapped.versions().get(1);

    assertThat(v2.terms().subList(0, 2)).extracting(MappedTerm::eventKey)
        .containsExactly(v1.terms().get(0).eventKey(), v1.terms().get(1).eventKey());
    assertThat(v2.terms().get(2).eventKey()).isNotIn(keys(v1));
    assertThat(v2.expiry().eventKey()).isEqualTo(v1.expiry().eventKey());
    // The yearly rate is one LMS rate per term: the first two are the same as before, the third term's is new
    assertThat(v1.rates()).extracting(MappedRate::termNo).containsExactly(1, 2);
    assertThat(v2.rates()).extracting(MappedRate::termNo).containsExactly(1, 2, 3);
    assertThat(v2.rates().subList(0, 2)).extracting(MappedRate::eventKey)
        .containsExactlyElementsOf(v1.rates().stream().map(MappedRate::eventKey).toList());
    assertThat(v2.rates().get(2).eventKey()).isNotIn(keys(v1));
    assertThat(v2.otherEvents().subList(0, 2)).extracting(MappedOtherEvent::eventKey)
        .containsExactly(v1.otherEvents().get(0).eventKey(), v1.otherEvents().get(1).eventKey());
    assertThat(v2.otherEvents().get(2).eventKey()).isNotIn(keys(v1));
    assertThat(keys(v2)).doesNotHaveDuplicates();
    assertThat(mapped.notes().noteCountsByReason()).doesNotContainKey(EventKeys.MATCHED_IN_ORDER);
  }

  @Test
  void map_comments_areNewOnlyWhenTheyChangeFromThePreviousVersion() {
    var first = version(1, 2014, false,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments("Initial term"),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150").withComments("Initial rental")
    );
    var second = version(2, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0).withComments(" Initial term "),
        term("2", LocalDate.of(2028, Month.DECEMBER, 1)).withOffset(14, 0, 0).withComments("Second term extended"),
        event("R", START).withOffset(0, 0, 0).withYearlyRate("150").withComments("Initial rental")
    );

    var mapped = map(first, second);
    var v1 = mapped.versions().get(0);
    var v2 = mapped.versions().get(1);

    assertThat(v1.terms()).extracting(MappedTerm::comment)
        .containsExactly(new MappedComment("Initial term", true), null);
    assertThat(v2.terms()).extracting(MappedTerm::comment)
        .containsExactly(new MappedComment("Initial term", false), new MappedComment("Second term extended", true));
    // A PEARS rate's comments go on its first LMS rate only
    assertThat(v1.rates()).extracting(MappedRate::comment)
        .containsExactly(new MappedComment("Initial rental", true), null);
    assertThat(v2.rates()).extracting(MappedRate::comment)
        .containsExactly(new MappedComment("Initial rental", false), null);
  }

  @Test
  void map_eventsThatCannotBeToldApart_areMatchedInOrderAndNoted() {
    var first = version(1, 2014, false,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0).withComments("Well A"),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0).withComments("Well B")
    );
    var second = version(2, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0).withComments("Well A"),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0).withComments("Well B")
    );

    var mapped = map(first, second);

    assertThat(mapped.versions().get(1).otherEvents()).extracting(MappedOtherEvent::eventKey)
        .containsExactlyElementsOf(mapped.versions().get(0).otherEvents().stream().map(MappedOtherEvent::eventKey).toList());
    assertThat(mapped.notes().noteCountsByReason()).containsEntry(EventKeys.MATCHED_IN_ORDER, 1);
  }

  @Test
  void map_movedEventsWhoseCommentsAreNotUnique_areNew() {
    var first = version(1, 2014, false,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0).withComments("Drill a well"),
        event("BM", LocalDate.of(2017, Month.DECEMBER, 1)).withOffset(3, 0, 0).withComments("Drill a well")
    );
    var second = version(2, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.JUNE, 1)).withOffset(1, 6, 0).withComments("Drill a well"),
        event("BM", LocalDate.of(2017, Month.JUNE, 1)).withOffset(2, 6, 0).withComments("Drill a well")
    );

    var mapped = map(first, second);

    assertThat(mapped.versions().get(1).otherEvents()).extracting(MappedOtherEvent::eventKey)
        .doesNotContainAnyElementsOf(keys(mapped.versions().get(0)));
  }

  @Test
  void map_versionLeftOut_isSkippedAndTheNextIsMatchedToTheOneBefore() {
    var first = version(1, 2014, false,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0)
    );
    var leftOut = version(2, 2016, false,
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0)
    );
    var third = version(3, 2019, true,
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)).withOffset(4, 0, 0),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withOffset(2, 0, 0)
    );

    var mapped = map(first, leftOut, third);

    assertThat(mapped.leftOutVersions()).extracting(MappedSchedule.LeftOutVersion::plsdId).containsExactly(2L);
    assertThat(keys(mapped.versions().get(1))).containsExactlyElementsOf(keys(mapped.versions().get(0)));
  }

  private MappedSchedule map(PearsScheduleVersion... versions) {
    return mapper.map(new PearsSchedule(new PearsLicenceReference("P", 2244), List.of(versions)), EVENT_TYPES);
  }

  private static PearsScheduleVersion version(long plsdId, int savedYear, boolean current, PearsEventFixture... events) {
    return new PearsScheduleVersion(
        plsdId,
        LocalDateTime.of(savedYear, Month.JANUARY, 1, 9, 0),
        current ? null : LocalDateTime.of(savedYear + 1, Month.JANUARY, 1, 9, 0),
        START,
        Stream.of(events).map(PearsEventFixture::build).toList()
    );
  }

  private static List<Integer> keys(MappedScheduleVersion version) {
    return Stream.of(
            version.terms().stream().map(MappedTerm::eventKey),
            version.phases().stream().map(MappedPhase::eventKey),
            Stream.ofNullable(version.expiry()).map(MappedExpiry::eventKey),
            version.rates().stream().map(MappedRate::eventKey),
            version.otherEvents().stream().map(MappedOtherEvent::eventKey)
        )
        .flatMap(keys -> keys)
        .toList();
  }
}
