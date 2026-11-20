package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;

@ExtendWith(MockitoExtension.class)
class PearsScheduleTest {

  private static final PearsLicenceReference P1161 = new PearsLicenceReference("P", 1161);

  @Test
  void currentVersion_isTheVersionWithNoEndDate() {
    var current = version(2, null);

    var schedule = new PearsSchedule(P1161, List.of(version(1, LocalDateTime.of(2011, Month.JANUARY, 1, 9, 0)), current));

    assertThat(schedule.currentVersion()).contains(current);
  }

  @Test
  void currentVersion_whenEveryVersionIsEndDated_thenThereIsNone() {
    var schedule = new PearsSchedule(P1161, List.of(
        version(1, LocalDateTime.of(2006, Month.JANUARY, 1, 9, 0)),
        version(2, LocalDateTime.of(2011, Month.OCTOBER, 14, 14, 13, 29))
    ));

    assertThat(schedule.currentVersion()).isEmpty();
  }

  @Test
  void currentVersion_whenMoreThanOneVersionIsCurrent_thenThrows() {
    var schedule = new PearsSchedule(P1161, List.of(version(1, null), version(2, null)));

    assertThatThrownBy(schedule::currentVersion)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("P1161 has more than one current schedule version: 1 and 2");
  }

  @Test
  void versionsAndEvents_cannotBeChangedOnceRead() {
    var events = new ArrayList<PearsScheduleEvent>();
    var version = new PearsScheduleVersion(
        1, LocalDateTime.of(2004, Month.DECEMBER, 1, 9, 0), null, LocalDate.of(2004, Month.DECEMBER, 1), events);
    events.add(event());

    var versionEvents = version.events();
    var event = event();

    assertThat(versionEvents).isEmpty();
    assertThatThrownBy(() -> versionEvents.add(event)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void area_whenTheEventHoldsNoPartOfOne_thenIsNull() {
    assertThat(PearsScheduleEvent.Area.of(null, null, null, null)).isNull();
    assertThat(PearsScheduleEvent.Area.of(null, "A1", null, null))
        .isEqualTo(new PearsScheduleEvent.Area(null, "A1", null, null));
  }

  private static PearsScheduleVersion version(long plsdId, LocalDateTime end) {
    return new PearsScheduleVersion(
        plsdId, LocalDateTime.of(2004, Month.DECEMBER, 1, 9, 0), end, LocalDate.of(2004, Month.DECEMBER, 1), List.of());
  }

  private static PearsScheduleEvent event() {
    return new PearsScheduleEvent(1, "T", "1", null, "4", "0", "0", LocalDate.of(2008, Month.DECEMBER, 1),
        null, null, null, null, null, null, null, null, null);
  }
}
