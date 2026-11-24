package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent.LicenceScheduleAreaEvent;

class TimelineAreaEventViewTest {

  private static final LocalDate EVENT_DATE = LocalDate.of(2025, Month.MARCH, 1);

  @Test
  void getScheduleEventFrom_firstVersionOfAnArea_thenHeadingIsNameAndShortName() {
    var view = view(areaEvent("RD", "Area one", "A1", "1", "Approval: 01-03-2025"), EVENT_DATE);

    assertThat(view.heading()).isEqualTo("Area one (A1)");
    assertThat(view.description()).isEqualTo("Approval: 01-03-2025");
    assertThat(view.eventDateString()).isEqualTo("1 March 2025");
    assertThat(view.getEventType()).isEqualTo(ScheduleEventType.AREA);
    assertThat(view.getSortingDate()).isEqualTo(EVENT_DATE);
  }

  @Test
  void getScheduleEventFrom_laterVersionOfAnArea_thenShortNameCarriesTheVersion() {
    var view = view(areaEvent("RD", "Area one", "A1", "2", "Approval: 01-03-2025"), EVENT_DATE);

    assertThat(view.heading()).isEqualTo("Area one (A1v2)");
  }

  @Test
  void getScheduleEventFrom_noVersion_thenShortNameAsItIs() {
    var view = view(areaEvent("PA", "Area one", "A1", null, null), EVENT_DATE);

    assertThat(view.heading()).isEqualTo("Area one (A1)");
    assertThat(view.description()).isEmpty();
  }

  @Test
  void getScheduleEventFrom_handEnteredPreProducingAreaEvent_thenGenericHeading() {
    var view = view(areaEvent("PA", null, null, null, "Pre-producing area approved"), EVENT_DATE);

    assertThat(view.heading()).isEqualTo(TimelineAreaEventView.PRE_PRODUCING_AREA);
    assertThat(view.description()).isEqualTo("Pre-producing area approved");
  }

  @Test
  void getScheduleEventFrom_onshoreEventWithNoArea_thenGenericHeading() {
    var view = view(areaEvent("RD", null, null, null, "Retention area approved"), EVENT_DATE);

    assertThat(view.heading()).isEqualTo(TimelineAreaEventView.RETENTION_OR_DEVELOPMENT_AREA);
  }

  @Test
  void getScheduleEventFrom_areaTypes_thenTheKindOfArea() {
    assertThat(view(typedAreaEvent("RD", "RETENTION"), EVENT_DATE).areaType()).isEqualTo(TimelineAreaEventView.RETENTION_AREA);
    assertThat(view(typedAreaEvent("RD", "DEVELOPMENT"), EVENT_DATE).areaType())
        .isEqualTo(TimelineAreaEventView.DEVELOPMENT_AREA);
    assertThat(view(typedAreaEvent("PA", "PROSPECTIVE"), EVENT_DATE).areaType())
        .isEqualTo(TimelineAreaEventView.PRE_PRODUCING_AREA);
  }

  @Test
  void getScheduleEventFrom_noAreaType_thenTheKindFromTheEventCode() {
    assertThat(view(typedAreaEvent("RD", null), EVENT_DATE).areaType())
        .isEqualTo(TimelineAreaEventView.RETENTION_OR_DEVELOPMENT_AREA);
    assertThat(view(typedAreaEvent("PA", " "), EVENT_DATE).areaType()).isEqualTo(TimelineAreaEventView.PRE_PRODUCING_AREA);
  }

  @Test
  void getScheduleEventFrom_noArea_thenNoKindAsTheHeadingSaysIt() {
    var view = view(areaEvent("PA", null, null, null, "Pre-producing area approved"), EVENT_DATE);

    assertThat(view.heading()).isEqualTo(TimelineAreaEventView.PRE_PRODUCING_AREA);
    assertThat(view.areaType()).isEmpty();
  }

  @Test
  void getScheduleEventFrom_showsProgressUpToTheProgressDate() {
    var areaEvent = areaEvent("RD", "Area one", "A1", "1", null);

    assertThat(view(areaEvent, EVENT_DATE).showProgress()).isTrue();
    assertThat(view(areaEvent, EVENT_DATE.minusDays(1)).showProgress()).isFalse();
  }

  private static TimelineAreaEventView view(LicenceScheduleAreaEvent areaEvent, LocalDate finalProgressDate) {
    return (TimelineAreaEventView) TimelineAreaEventView.getScheduleEventFrom(areaEvent, finalProgressDate);
  }

  private static LicenceScheduleAreaEvent typedAreaEvent(String eventMnem, String areaType) {
    var areaEvent = areaEvent(eventMnem, "Area one", "A1", "1", null);
    areaEvent.setAreaType(areaType);
    return areaEvent;
  }

  private static LicenceScheduleAreaEvent areaEvent(
      String eventMnem,
      String areaName,
      String areaShortName,
      String areaVersion,
      String comments
  ) {
    var areaEvent = new LicenceScheduleAreaEvent();
    areaEvent.setEventMnem(eventMnem);
    areaEvent.setEventDate(EVENT_DATE);
    areaEvent.setAreaName(areaName);
    areaEvent.setAreaShortName(areaShortName);
    areaEvent.setAreaVersion(areaVersion);
    areaEvent.setComments(comments);
    return areaEvent;
  }
}
