package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEvent;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;

@ExtendWith(MockitoExtension.class)
class OtherScheduleEventDeadlineServiceTest {

  private static final LocalDate EVENT_DATE = LocalDate.of(2027, Month.MARCH, 31);
  private static final LocalDate NOTICE_DATE = LocalDate.of(2026, Month.OCTOBER, 1);
  private static final LocalDate WINDOW_END = LocalDate.of(2027, Month.APRIL, 1);
  private static final String DESCRIPTION = "Relinquish 50% of the licensed area";

  @Mock
  private Clock clock;

  @Mock
  private OtherScheduleEventRepository otherScheduleEventRepository;

  @Mock
  private OtherScheduleEventService otherScheduleEventService;

  @InjectMocks
  private OtherScheduleEventDeadlineService otherScheduleEventDeadlineService;

  private Licence licence;
  private LicenceSchedule licenceSchedule;
  private LicenceScheduleDetail scheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
    licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    scheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();
  }

  @Test
  void getDeadlinesDueReminder_whenNothingIsInTheNoticeWindow_thenNothingIsDue() {
    mockWindow(NOTICE_DATE, WINDOW_END, List.of());

    assertThat(otherScheduleEventDeadlineService.getDeadlinesDueReminder()).isEmpty();

    verifyNoInteractions(otherScheduleEventService);
  }

  @Test
  void getDeadlinesDueReminder_whenAnEventIsOnItsNoticeDate_thenItIsDueWithItsEventIdentity() {
    var event = event();
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(event));
    when(otherScheduleEventService.resolveOtherScheduleEventDate(event)).thenReturn(EVENT_DATE);

    var deadlines = otherScheduleEventDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(expectedDeadline(event));
  }

  @Test
  void getDeadlinesDueReminder_whenTheEventHasNoDescription_thenTheCategoryAloneNamesIt() {
    var event = event();
    event.setDescription(null);
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(event));
    when(otherScheduleEventService.resolveOtherScheduleEventDate(event)).thenReturn(EVENT_DATE);

    var deadlines = otherScheduleEventDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(new ReminderDeadline(
        event,
        event.getOriginalEventId(),
        licence,
        EVENT_DATE,
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName(),
        ReminderType.OTHER_SCHEDULE_EVENT));
  }

  @Test
  void getDeadlinesDueReminder_whenTheEventHasNoCategory_thenItIsSkippedRatherThanFailingTheRun() {
    var event = event();
    event.setCategory(null);
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(event));

    assertThat(otherScheduleEventDeadlineService.getDeadlinesDueReminder()).isEmpty();

    verifyNoInteractions(otherScheduleEventService);
  }

  @Test
  void getDeadlinesDueReminder_theDayBeforeTheNoticeDate_thenNothingIsDue() {
    var event = event();
    mockWindow(LocalDate.of(2026, Month.SEPTEMBER, 30), LocalDate.of(2027, Month.MARCH, 30), List.of(event));
    when(otherScheduleEventService.resolveOtherScheduleEventDate(event)).thenReturn(EVENT_DATE);

    assertThat(otherScheduleEventDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  private void mockWindow(LocalDate today, LocalDate latestEventDate, List<OtherScheduleEvent> relativeDateEvents) {
    when(clock.instant()).thenReturn(today.atStartOfDay().toInstant(ZoneOffset.UTC));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(otherScheduleEventRepository
        .findAllByDateOptionAndEventDateBetweenAndLicenceScheduleDetail_Status(
            OtherScheduleEventDateOption.RELATIVE_DATE, today, latestEventDate, LicenceScheduleDetailStatus.ACTIVE))
        .thenReturn(relativeDateEvents);
  }

  private OtherScheduleEvent event() {
    var event = new OtherScheduleEvent();
    event.setId(UUID.randomUUID());
    event.setOriginalEventId(UUID.randomUUID());
    event.setLicenceSchedule(licenceSchedule);
    event.setLicenceScheduleDetail(scheduleDetail);
    event.setCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    event.setDescription(DESCRIPTION);
    event.setDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    return event;
  }

  private ReminderDeadline expectedDeadline(OtherScheduleEvent event) {
    return new ReminderDeadline(
        event,
        event.getOriginalEventId(),
        licence,
        EVENT_DATE,
        OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.getDisplayName() + ": " + DESCRIPTION,
        ReminderType.OTHER_SCHEDULE_EVENT);
  }
}
