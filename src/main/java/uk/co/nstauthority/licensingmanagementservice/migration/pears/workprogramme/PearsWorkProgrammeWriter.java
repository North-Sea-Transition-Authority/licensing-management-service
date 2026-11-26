package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker.CrossLicenceEventTrackerService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleComment;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.schedulecomments.ScheduleCommentRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeStatus;

/**
 * Saves a licence's mapped work programme onto its migrated schedule's {@code ACTIVE} detail, in the caller's
 * transaction (plan, design section 3). No new schedule version is created.
 */
@Service
class PearsWorkProgrammeWriter {

  private static final ZoneId PEARS_ZONE = ZoneId.of("Europe/London");

  private final MigratedScheduleReader migratedScheduleReader;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository;
  private final EventCommentRepository eventCommentRepository;
  private final ScheduleCommentRepository scheduleCommentRepository;
  private final PearsWorkProgrammeActivityMigrationLinkRepository activityMigrationLinkRepository;
  private final PearsWorkProgrammeScheduleCommentLinkRepository scheduleCommentLinkRepository;
  private final PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final CrossLicenceEventTrackerService crossLicenceEventTrackerService;
  private final EntityManager entityManager;
  private final Clock clock;

  @SuppressWarnings("java:S107") // One collaborator per part of the work programme it saves
  PearsWorkProgrammeWriter(
      MigratedScheduleReader migratedScheduleReader,
      WorkProgrammeActivityService workProgrammeActivityService,
      WorkProgrammeActivityStatusRepository workProgrammeActivityStatusRepository,
      EventCommentRepository eventCommentRepository,
      ScheduleCommentRepository scheduleCommentRepository,
      PearsWorkProgrammeActivityMigrationLinkRepository activityMigrationLinkRepository,
      PearsWorkProgrammeScheduleCommentLinkRepository scheduleCommentLinkRepository,
      PearsWorkProgrammeMigratedLicenceRepository migratedLicenceRepository,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      CrossLicenceEventTrackerService crossLicenceEventTrackerService,
      EntityManager entityManager,
      Clock clock
  ) {
    this.migratedScheduleReader = migratedScheduleReader;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.workProgrammeActivityStatusRepository = workProgrammeActivityStatusRepository;
    this.eventCommentRepository = eventCommentRepository;
    this.scheduleCommentRepository = scheduleCommentRepository;
    this.activityMigrationLinkRepository = activityMigrationLinkRepository;
    this.scheduleCommentLinkRepository = scheduleCommentLinkRepository;
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.crossLicenceEventTrackerService = crossLicenceEventTrackerService;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  /**
   * Saves the activities with their status histories and comments, the schedule comments, and the records the
   * comparison report and cleardown use, then works out the due dates LMS stores.
   *
   * @param runId the migration run, recorded against the licence
   * @return false when the licence's work programme was already migrated. The licence's row is locked and the check made
   *     here, in the caller's transaction, so two migrations of the same licence at once can't both write it
   * @throws IllegalStateException if the licence has no migrated schedule, which the caller checks first
   */
  boolean write(Licence licence, MappedWorkProgramme mappedWorkProgramme, UUID runId) {
    entityManager.find(Licence.class, licence.getId(), LockModeType.PESSIMISTIC_WRITE);
    var active = migratedScheduleReader.read(licence)
        .orElseThrow(() -> new IllegalStateException(
            "%s has no migrated schedule".formatted(licence.getLicenceReference())));
    var licenceSchedule = active.detail().getLicenceSchedule();
    if (migratedLicenceRepository.existsById(licenceSchedule.getId())) {
      return false;
    }

    var activities = new ArrayList<WorkProgrammeActivity>();
    for (var mappedActivity : mappedWorkProgramme.activities()) {
      activities.add(activity(active, mappedActivity));
    }
    workProgrammeActivityService.saveWorkProgrammeActivities(activities);

    var statuses = new ArrayList<WorkProgrammeActivityStatus>();
    var comments = new ArrayList<EventComment>();
    var links = new ArrayList<PearsWorkProgrammeActivityMigrationLink>();
    for (var i = 0; i < activities.size(); i++) {
      var activity = activities.get(i);
      var mappedActivity = mappedWorkProgramme.activities().get(i);
      var mappedStatuses = mappedActivity.history().statuses();
      var appliedInstants = appliedInstants(mappedStatuses);
      for (var j = 0; j < mappedStatuses.size(); j++) {
        statuses.add(status(activity, mappedStatuses.get(j), appliedInstants.get(j)));
      }
      mappedActivity.history().comments().forEach(comment -> comments.add(eventComment(activity, comment)));
      links.add(new PearsWorkProgrammeActivityMigrationLink(
          activity.getId(),
          mappedActivity.pedWpId(),
          mappedActivity.pedWpdId(),
          mappedActivity.activity().pearsActivityId(),
          mappedActivity.activity().source().activityType()
      ));
    }
    workProgrammeActivityStatusRepository.saveAll(statuses);
    eventCommentRepository.saveAll(comments);
    activityMigrationLinkRepository.saveAll(links);

    var scheduleComments = mappedWorkProgramme.scheduleComments().stream()
        .map(comment -> {
          var scheduleComment = new ScheduleComment();
          scheduleComment.setLicenceSchedule(licenceSchedule);
          scheduleComment.setComment(comment.text());
          scheduleComment.setTimestamp(instant(comment.timestamp()));
          return scheduleComment;
        })
        .toList();
    scheduleCommentRepository.saveAll(scheduleComments);
    scheduleCommentLinkRepository.saveAll(scheduleComments.stream()
        .map(comment -> new PearsWorkProgrammeScheduleCommentLink(comment.getId()))
        .toList());

    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(active.detail());
    crossLicenceEventTrackerService.refreshScheduleCache(active.detail());

    migratedLicenceRepository.save(new PearsWorkProgrammeMigratedLicence(
        licenceSchedule.getId(), licence.getId(), runId, Instant.now(clock)));
    return true;
  }

  private static WorkProgrammeActivity activity(ActiveScheduleDetail active, MappedWorkProgramme.Activity mapped) {
    var source = mapped.activity();
    var date = mapped.date();

    var activity = new WorkProgrammeActivity();
    activity.setLicenceScheduleDetail(active.detail());
    activity.setLicenceSchedule(active.detail().getLicenceSchedule());
    activity.setCategory(source.category());
    activity.setOtherCategoryName(source.otherCategoryName());
    activity.setCommitment(source.commitment());
    activity.setDescription(source.description());
    activity.setDateOption(date.dateOption());
    if (date.termType() != null) {
      activity.setLicenceScheduleTerm(active.terms().stream()
          .filter(term -> term.getTermType() == date.termType())
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No %s term on the schedule".formatted(date.termType()))));
    }
    if (date.phaseType() != null) {
      activity.setLicenceSchedulePhase(active.phases().stream()
          .filter(phase -> phase.getPhaseType() == date.phaseType())
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No %s on the schedule".formatted(date.phaseType()))));
    }
    activity.setRelativeDuration(date.relativeDuration());
    return activity;
  }

  private static WorkProgrammeActivityStatus status(
      WorkProgrammeActivity activity,
      MappedActivityHistory.Status mappedStatus,
      Instant appliedAt
  ) {
    var status = new WorkProgrammeActivityStatus();
    status.setScheduleEvent(activity);
    status.setStatus(lmsStatus(mappedStatus.status()));
    status.setAppliedDatetime(appliedAt);
    return status;
  }

  /**
   * The times the statuses were applied, in PEARS order. LMS takes an activity's latest status by time, so a status
   * applied at or before the one before it, such as two notes saved in the same second, is put 1 ms after it.
   */
  static List<Instant> appliedInstants(List<MappedActivityHistory.Status> statuses) {
    var instants = new ArrayList<Instant>();
    Instant previous = null;
    for (var status : statuses) {
      var appliedAt = instant(status.appliedAt());
      if (previous != null && !appliedAt.isAfter(previous)) {
        appliedAt = previous.plusMillis(1);
      }
      instants.add(appliedAt);
      previous = appliedAt;
    }
    return instants;
  }

  private static EventComment eventComment(WorkProgrammeActivity activity, MappedActivityHistory.Comment comment) {
    var eventComment = new EventComment();
    eventComment.setScheduleEvent(activity);
    eventComment.setComment(comment.text());
    eventComment.setStatus(EventCommentStatus.PUBLISHED);
    eventComment.setTimestamp(instant(comment.timestamp()));
    return eventComment;
  }

  static WorkProgrammeStatus lmsStatus(MigratedStatus status) {
    return WorkProgrammeStatus.valueOf(status.name());
  }

  static Instant instant(LocalDateTime pearsTime) {
    return pearsTime.atZone(PEARS_ZONE).toInstant();
  }
}
