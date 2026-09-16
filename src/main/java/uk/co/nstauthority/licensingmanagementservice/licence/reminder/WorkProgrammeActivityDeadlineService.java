package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;

@Service
public class WorkProgrammeActivityDeadlineService implements ReminderDeadlineSource {

  static final Set<WorkProgrammeStatus> CLOSED_STATUSES =
      Set.of(WorkProgrammeStatus.COMPLETE, WorkProgrammeStatus.FULL_WAIVER, WorkProgrammeStatus.TRANSFERRED);

  static final String DISPLAY_NAME_FORMAT = "%s: %s";

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkProgrammeActivityDeadlineService.class);

  private final Clock clock;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  public WorkProgrammeActivityDeadlineService(
      Clock clock,
      WorkProgrammeActivityService workProgrammeActivityService,
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService
  ) {
    this.clock = clock;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.workProgrammeActivityStatusService = workProgrammeActivityStatusService;
  }

  @Override
  public List<ReminderDeadline> getDeadlinesDueReminder() {
    var noticePeriod = ReminderType.WORK_PROGRAMME_ACTIVITY.getNoticePeriod();
    var today = LocalDate.now(clock);
    var latestDueDate = noticePeriod.getLatestDeadlineDate(today);

    var candidateActivities = workProgrammeActivityService
        .getRelativeDateActivitiesDueBetweenOnActiveSchedules(today, latestDueDate);

    var closedActivityIds = workProgrammeActivityStatusService
        .getActivityIdsWithLatestStatusIn(candidateActivities, CLOSED_STATUSES);

    return candidateActivities.stream()
        .filter(this::hasCategory)
        .filter(activity -> isDue(activity, noticePeriod, today))
        .filter(activity -> !closedActivityIds.contains(activity.getOriginalEventId()))
        .map(this::toDeadline)
        .toList();
  }

  private boolean hasCategory(WorkProgrammeActivity activity) {
    if (activity.getCategory() == null) {
      LOGGER.warn("Skipping work programme activity {} for reminders as it has no category", activity.getId());
      return false;
    }

    return true;
  }

  private boolean isDue(WorkProgrammeActivity activity, NoticePeriod noticePeriod, LocalDate today) {
    var dueDate = getDueDate(activity);

    if (dueDate == null) {
      LOGGER.warn("Skipping work programme activity {} for reminders as it has no due date", activity.getId());
      return false;
    }

    return noticePeriod.isWithinNoticeWindow(dueDate, today);
  }

  private LocalDate getDueDate(WorkProgrammeActivity activity) {
    return workProgrammeActivityService.resolveWorkProgrammeActivityDueDate(activity);
  }

  private String getDisplayName(WorkProgrammeActivity activity) {
    var caption = activity.getEventCaption();

    if (StringUtils.isBlank(activity.getDescription())) {
      return caption;
    }

    return DISPLAY_NAME_FORMAT.formatted(caption, activity.getDescription());
  }

  private ReminderDeadline toDeadline(WorkProgrammeActivity activity) {
    return new ReminderDeadline(
        activity,
        activity.getOriginalEventId(),
        activity.getLicence(),
        getDueDate(activity),
        getDisplayName(activity),
        ReminderType.WORK_PROGRAMME_ACTIVITY);
  }
}
