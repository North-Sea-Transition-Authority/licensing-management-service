package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusRepository;

/**
 * Reads a migrated schedule's work programme as stored, for the comparison report. Nothing is recalculated.
 */
@Service
class LmsWorkProgrammeReader {

  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;
  private final EventCommentRepository eventCommentRepository;
  private final ScheduleCommentRepository scheduleCommentRepository;
  private final PearsWorkProgrammeActivityMigrationLinkRepository activityMigrationLinkRepository;
  private final PearsWorkProgrammeScheduleCommentLinkRepository scheduleCommentLinkRepository;

  LmsWorkProgrammeReader(
      WorkProgrammeActivityService workProgrammeActivityService,
      WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository,
      EventCommentRepository eventCommentRepository,
      ScheduleCommentRepository scheduleCommentRepository,
      PearsWorkProgrammeActivityMigrationLinkRepository activityMigrationLinkRepository,
      PearsWorkProgrammeScheduleCommentLinkRepository scheduleCommentLinkRepository
  ) {
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.workProgrammeActivityStatusRepository = workProgrammeActivityStatusRepository;
    this.eventCommentRepository = eventCommentRepository;
    this.scheduleCommentRepository = scheduleCommentRepository;
    this.activityMigrationLinkRepository = activityMigrationLinkRepository;
    this.scheduleCommentLinkRepository = scheduleCommentLinkRepository;
  }

  LmsWorkProgramme read(ActiveScheduleDetail active) {
    var licenceSchedule = active.detail().getLicenceSchedule();
    var activities = workProgrammeActivityService.getWorkProgrammeActivities(active.detail());

    // A link names the activity as migrated, whose id is its originalEventId: later versions' copies keep that
    var links = activityMigrationLinkRepository.findAllById(
            activities.stream().map(WorkProgrammeActivity::getOriginalEventId).toList())
        .stream()
        .collect(Collectors.toMap(PearsWorkProgrammeActivityMigrationLink::getScheduleEventId, Function.identity()));
    var statuses = workProgrammeActivityStatusRepository.findAllByScheduleEvent_OriginalEventIdIn(
            activities.stream().map(WorkProgrammeActivity::getOriginalEventId).toList())
        .stream()
        .sorted(Comparator.comparing(WorkProgrammeActivityStatus::getAppliedDatetime))
        .collect(Collectors.groupingBy(status -> status.getScheduleEvent().getOriginalEventId()));
    var comments = eventCommentRepository
        .getAllByScheduleEvent_LicenceScheduleAndStatus(licenceSchedule, EventCommentStatus.PUBLISHED).stream()
        .sorted(Comparator.comparing(EventComment::getTimestamp))
        .collect(Collectors.groupingBy(comment -> comment.getScheduleEvent().getOriginalEventId()));

    var lmsActivities = activities.stream()
        .map(activity -> activity(activity, links.get(activity.getOriginalEventId()), statuses, comments))
        .toList();

    var scheduleComments = scheduleCommentRepository.findAllByLicenceScheduleOrderByTimestamp(licenceSchedule);
    Set<UUID> migratedComments = scheduleCommentLinkRepository.findAllById(
            scheduleComments.stream().map(ScheduleComment::getId).toList()).stream()
        .map(PearsWorkProgrammeScheduleCommentLink::getScheduleCommentId)
        .collect(Collectors.toSet());

    return new LmsWorkProgramme(
        lmsActivities,
        scheduleComments.stream()
            .map(comment -> new LmsWorkProgramme.ScheduleComment(
                comment.getComment(), migratedComments.contains(comment.getId())))
            .toList()
    );
  }

  private static LmsWorkProgramme.Activity activity(
      WorkProgrammeActivity activity,
      PearsWorkProgrammeActivityMigrationLink link,
      Map<UUID, List<WorkProgrammeActivityStatus>> statuses,
      Map<UUID, List<EventComment>> comments
  ) {
    return new LmsWorkProgramme.Activity(
        activity.getId(),
        link == null ? null : link.getPearsWpId(),
        link == null ? null : link.getPearsActivityId(),
        activity.getCategory(),
        activity.getOtherCategoryName(),
        activity.getCommitment(),
        activity.getDescription(),
        activity.getDateOption(),
        activity.getLicenceScheduleTerm() == null ? null : activity.getLicenceScheduleTerm().getTermType(),
        activity.getLicenceSchedulePhase() == null ? null : activity.getLicenceSchedulePhase().getPhaseType(),
        activity.getRelativeDuration(),
        activity.getDueDate(),
        statuses.getOrDefault(activity.getOriginalEventId(), List.of()).stream()
            .map(status -> new LmsWorkProgramme.Status(status.getStatus(), status.getAppliedDatetime()))
            .toList(),
        comments.getOrDefault(activity.getOriginalEventId(), List.of()).stream()
            .map(EventComment::getComment)
            .toList()
    );
  }
}
