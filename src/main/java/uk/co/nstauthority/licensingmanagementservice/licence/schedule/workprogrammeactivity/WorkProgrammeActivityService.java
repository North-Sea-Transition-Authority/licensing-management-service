package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@Service
public class WorkProgrammeActivityService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;
  private final WorkProgrammeActivityStatusService workProgrammeActivityStatusService;
  private final EventCommentService eventCommentService;

  public WorkProgrammeActivityService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository,
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService,
      EventCommentService eventCommentService
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
    this.workProgrammeActivityStatusService = workProgrammeActivityStatusService;
    this.eventCommentService = eventCommentService;
  }

  public WorkProgrammeActivity getWorkProgrammeActivityByIdOrThrow(UUID id) {
    return workProgrammeActivityRepository.findById(id)
          .orElseThrow(() -> new LmsEntityNotFoundException("WorkProgrammeActivity not found", id.toString()));
  }

  public Map<UUID, WorkProgrammeActivity> getWorkProgrammeActivitiesByIds(Collection<UUID> ids) {
    return workProgrammeActivityRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(WorkProgrammeActivity::getId, Function.identity()));
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivities(LicenceScheduleDetail licenceScheduleDetail) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivitiesByTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      WorkProgrammeActivityDateOption dateOption
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleTermAndDateOption(licenceScheduleTerm, dateOption);
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivitiesByPhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      WorkProgrammeActivityDateOption dateOption
  ) {
    return workProgrammeActivityRepository.findAllByLicenceSchedulePhaseAndDateOption(licenceSchedulePhase, dateOption);
  }

  @Transactional
  public void saveWorkProgrammeActivities(List<WorkProgrammeActivity> workProgrammeActivities) {
    workProgrammeActivityRepository.saveAll(workProgrammeActivities);
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivitiesByDateRangeFor(LicenceScheduleTerm licenceScheduleTerm) {
    return getWorkProgrammeActivitiesByDateRange(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivitiesByDateRangeFor(LicenceSchedulePhase licenceSchedulePhase) {
    return getWorkProgrammeActivitiesByDateRange(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    );
  }

  private List<WorkProgrammeActivity> getWorkProgrammeActivitiesByDateRange(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate from,
      LocalDate to
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndDueDateBetween(licenceScheduleDetail, from, to);
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivitiesAfterDate(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndDueDateAfter(licenceScheduleDetail, date);
  }

  public List<WorkProgrammeActivity> getAllActivitiesLinkedTo(LicenceScheduleTerm licenceScheduleTerm) {
    return workProgrammeActivityRepository.findByLicenceScheduleTerm(licenceScheduleTerm);
  }

  public List<WorkProgrammeActivity> getAllActivitiesLinkedTo(LicenceSchedulePhase licenceSchedulePhase) {
    return workProgrammeActivityRepository.findByLicenceSchedulePhase(licenceSchedulePhase);
  }

  @Transactional
  public void deleteWorkProgrammeActivity(WorkProgrammeActivity workProgrammeActivity) {
    eventCommentService.deletePendingCommentForScheduleEvent(workProgrammeActivity);
    if (workProgrammeActivityRepository.countByOriginalEventId(workProgrammeActivity.getOriginalEventId()) == 1) {
      workProgrammeActivityStatusService.deleteStatusesFor(workProgrammeActivity);
    }

    workProgrammeActivityRepository.delete(workProgrammeActivity);
  }

  public boolean hasActivitiesForPhase(LicenceSchedulePhase licenceSchedulePhase) {
    return workProgrammeActivityRepository.existsByLicenceSchedulePhase(licenceSchedulePhase);
  }

  public boolean hasActivitiesForTerm(LicenceScheduleTerm licenceScheduleTerm) {
    return workProgrammeActivityRepository.existsByLicenceScheduleTerm(licenceScheduleTerm);
  }

  public List<WorkProgrammeActivityView> getLicenceWorkProgramActivitiesViewsForGivenTerm(
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var workProgrammeActivities = workProgrammeActivityRepository.findByLicenceScheduleTerm(licenceScheduleTerm);
    return buildWorkProgrammeActivityViews(workProgrammeActivities);
  }

  public List<WorkProgrammeActivityView> getLicenceWorkProgramActivitiesViewsForGivenPhase(
      LicenceSchedulePhase licenceSchedulePhase
  ) {
    var workProgrammeActivities = workProgrammeActivityRepository.findByLicenceSchedulePhase(licenceSchedulePhase);
    return buildWorkProgrammeActivityViews(workProgrammeActivities);
  }

  public List<WorkProgrammeActivityView> getLicenceWorkProgramActivitiesViews(
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    List<WorkProgrammeActivity> workProgrammeActivities =
        workProgrammeActivityRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
    return buildWorkProgrammeActivityViews(workProgrammeActivities);
  }

  public WorkProgrammeActivityView createWorkProgrammeActivityView(
      WorkProgrammeActivity workProgrammeActivity
  ) {
    var latestActivityStatus = workProgrammeActivityStatusService.getLatestStatusFor(workProgrammeActivity);
    return createWorkProgrammeActivityView(workProgrammeActivity, latestActivityStatus);
  }

  private WorkProgrammeActivityView createWorkProgrammeActivityView(
      WorkProgrammeActivity workProgrammeActivity,
      WorkProgrammeActivityStatus status
  ) {
    LocalDate dueDate = resolveWorkProgrammeActivityDueDate(workProgrammeActivity);
    return new WorkProgrammeActivityView(
        workProgrammeActivity.getId().toString(),
        DateFormatUtil.convertToDisplayText(dueDate),
        resolveCategory(workProgrammeActivity),
        workProgrammeActivity.getDescription(),
        getCategoryWithDueDate(workProgrammeActivity, dueDate),
        workProgrammeActivity.getCommitment().getDisplayName(),
        status.getStatus()
    );
  }

  private List<WorkProgrammeActivityView> buildWorkProgrammeActivityViews(List<WorkProgrammeActivity> activities) {
    var statusByRef = workProgrammeActivityStatusService.getLatestStatusesFor(activities);
    return activities.stream()
        .map(activity -> createWorkProgrammeActivityView(activity, statusByRef.get(activity.getOriginalEventId())))
        .toList();
  }

  public String resolveCategory(WorkProgrammeActivity workProgrammeActivity) {
    if (workProgrammeActivity.getOtherCategoryName() == null) {
      return workProgrammeActivity.getCategory().getDisplayName();
    }
    return workProgrammeActivity.getOtherCategoryName();
  }

  public LocalDate resolveWorkProgrammeActivityDueDate(WorkProgrammeActivity activity) {
    WorkProgrammeActivityDateOption dateOption = activity.getDateOption();

    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
      return activity.getLicenceSchedulePhase().getEndDate();
    }
    if (dateOption.equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
      return activity.getLicenceScheduleTerm().getEndDate();
    }
    return activity.getDueDate();
  }

  public String getCategoryWithDueDate(WorkProgrammeActivity activity, LocalDate dueDate
  ) {
    return resolveCategory(activity) + " " + DateFormatUtil.convertToDisplayTextWithDueDateLabel(dueDate);
  }
}
