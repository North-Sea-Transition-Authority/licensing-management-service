package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityDeadlineServiceTest {

  private static final LocalDate DUE_DATE = LocalDate.of(2027, Month.MARCH, 31);
  private static final LocalDate NOTICE_DATE = LocalDate.of(2026, Month.OCTOBER, 1);
  private static final LocalDate WINDOW_END = LocalDate.of(2027, Month.APRIL, 1);
  private static final String DESCRIPTION = "Drill one exploration well";

  @Mock
  private Clock clock;

  @Mock
  private WorkProgrammeActivityService workProgrammeActivityService;

  @Mock
  private WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  @InjectMocks
  private WorkProgrammeActivityDeadlineService workProgrammeActivityDeadlineService;

  private Licence licence;
  private LicenceScheduleDetail scheduleDetail;

  @BeforeEach
  void setUp() {
    licence = LicenceTestUtil.builder().withId(1).withLicenceReference("P001").build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    scheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withStatus(LicenceScheduleDetailStatus.ACTIVE)
        .build();
  }

  @Test
  void getDeadlinesDueReminder_whenNothingIsInTheNoticeWindow_thenNothingIsDue() {
    mockWindow(NOTICE_DATE, WINDOW_END, List.of());

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenAnActivityIsOnItsNoticeDate_thenItIsDueWithItsEventIdentity() {
    var activity = activity();
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(activity));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity)).thenReturn(DUE_DATE);
    mockClosedActivities(List.of(activity), Set.of());

    var deadlines = workProgrammeActivityDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(expectedDeadline(activity));
  }

  @Test
  void getDeadlinesDueReminder_whenTheActivityHasNoDescription_thenTheCategoryAloneNamesIt() {
    var activity = activity();
    activity.setDescription(null);
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(activity));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity)).thenReturn(DUE_DATE);
    mockClosedActivities(List.of(activity), Set.of());

    var deadlines = workProgrammeActivityDeadlineService.getDeadlinesDueReminder();

    assertThat(deadlines).containsExactly(new ReminderDeadline(
        activity,
        activity.getOriginalEventId(),
        licence,
        DUE_DATE,
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName(),
        ReminderType.WORK_PROGRAMME_ACTIVITY));
  }

  @Test
  void getDeadlinesDueReminder_whenTheActivityHasNoCategory_thenItIsSkippedRatherThanFailingTheRun() {
    var activity = activity();
    activity.setCategory(null);
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(activity));

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenTheActivityHasNoDueDate_thenItIsSkippedRatherThanFailingTheRun() {
    var activity = activity();
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(activity));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity)).thenReturn(null);

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_theDayBeforeTheNoticeDate_thenNothingIsDue() {
    var activity = activity();
    mockWindow(LocalDate.of(2026, Month.SEPTEMBER, 30), LocalDate.of(2027, Month.MARCH, 30), List.of(activity));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity)).thenReturn(DUE_DATE);

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenTheDueDateHasAlreadyPassed_thenItIsNotDue() {
    var activity = activity();
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(activity));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity))
        .thenReturn(NOTICE_DATE.minusDays(1));

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenTheActivityIsClosed_thenItIsNotDue() {
    var activity = activity();
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(activity));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity)).thenReturn(DUE_DATE);
    mockClosedActivities(List.of(activity), Set.of(activity.getOriginalEventId()));

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder()).isEmpty();
  }

  @Test
  void getDeadlinesDueReminder_whenOneOfTwoActivitiesIsClosed_thenOnlyTheOtherIsDue() {
    var closed = activity();
    var outstanding = activity();
    mockWindow(NOTICE_DATE, WINDOW_END, List.of(closed, outstanding));
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(closed)).thenReturn(DUE_DATE);
    when(workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(outstanding)).thenReturn(DUE_DATE);
    mockClosedActivities(List.of(closed, outstanding), Set.of(closed.getOriginalEventId()));

    assertThat(workProgrammeActivityDeadlineService.getDeadlinesDueReminder())
        .containsExactly(expectedDeadline(outstanding));
  }

  private void mockWindow(LocalDate today, LocalDate latestDueDate, List<WorkProgrammeActivity> activities) {
    when(clock.instant()).thenReturn(today.atStartOfDay().toInstant(ZoneOffset.UTC));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(workProgrammeActivityService.getRelativeDateActivitiesDueBetweenOnActiveSchedules(today, latestDueDate))
        .thenReturn(activities);
  }

  private void mockClosedActivities(List<WorkProgrammeActivity> activities, Set<UUID> closedActivityIds) {
    when(workProgrammeActivityStatusService
        .getActivityIdsWithLatestStatusIn(activities, WorkProgrammeActivityDeadlineService.CLOSED_STATUSES))
        .thenReturn(closedActivityIds);
  }

  private WorkProgrammeActivity activity() {
    var activity = new WorkProgrammeActivity();
    activity.setId(UUID.randomUUID());
    activity.setOriginalEventId(UUID.randomUUID());
    activity.setLicenceSchedule(scheduleDetail.getLicenceSchedule());
    activity.setLicenceScheduleDetail(scheduleDetail);
    activity.setCategory(WorkProgrammeActivityCategory.DRILL_WELL);
    activity.setDescription(DESCRIPTION);
    activity.setDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    return activity;
  }

  private ReminderDeadline expectedDeadline(WorkProgrammeActivity activity) {
    return new ReminderDeadline(
        activity,
        activity.getOriginalEventId(),
        licence,
        DUE_DATE,
        WorkProgrammeActivityCategory.DRILL_WELL.getDisplayName() + ": " + DESCRIPTION,
        ReminderType.WORK_PROGRAMME_ACTIVITY);
  }
}
