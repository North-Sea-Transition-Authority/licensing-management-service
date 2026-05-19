package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status.WorkProgrammeActivityStatusService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.WorkProgrammeActivityView;

@Service
public class WorkProgrammeActivityService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;
  private final LicenceScheduleService licenceScheduleService;
  private final WorkProgrammeActivityStatusService workProgrammeActivityStatusService;

  public WorkProgrammeActivityService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository,
      LicenceScheduleService licenceScheduleService,
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
    this.licenceScheduleService = licenceScheduleService;
    this.workProgrammeActivityStatusService = workProgrammeActivityStatusService;
  }

  public WorkProgrammeActivity getWorkProgrammeActivityByIdOrThrow(UUID id) {
    return workProgrammeActivityRepository.findById(id)
          .orElseThrow(() -> new LmsEntityNotFoundException("WorkProgrammeActivity not found", id.toString()));
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivities(LicenceScheduleDetail licenceScheduleDetail) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      WorkProgrammeActivityDateOption dateOption
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleTermAndDateOption(licenceScheduleTerm, dateOption);
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByPhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      WorkProgrammeActivityDateOption dateOption
  ) {
    return workProgrammeActivityRepository.findAllByLicenceSchedulePhaseAndDateOption(licenceSchedulePhase, dateOption);
  }

  @Transactional
  public void saveWorkProgrammeActivities(List<WorkProgrammeActivity> workProgrammeActivities) {
    workProgrammeActivityRepository.saveAll(workProgrammeActivities);
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByDateRangeFor(LicenceScheduleTerm licenceScheduleTerm) {
    return getActiveWorkProgrammeActivitiesByDateRange(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByDateRangeFor(LicenceSchedulePhase licenceSchedulePhase) {
    return getActiveWorkProgrammeActivitiesByDateRange(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    );
  }

  private List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByDateRange(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate from,
      LocalDate to
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndDueDateBetween(licenceScheduleDetail, from, to);
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesAfterDate(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndDueDateAfter(licenceScheduleDetail, date);
  }

  @Transactional
  public void deleteWorkProgrammeActivity(WorkProgrammeActivity workProgrammeActivity) {
    workProgrammeActivityRepository.delete(workProgrammeActivity);
  }

  public List<WorkProgrammeActivityView> getCurrentWorkProgrammeActivitiesViews(
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var scheduleState = licenceScheduleService.getScheduleState(licenceScheduleDetail);

    if (scheduleState.currentPhase() != null) {
      return getLicenceWorkProgramActivitiesViewsForGivenPhase(scheduleState.currentPhase());
    }

    if (scheduleState.currentTerm() != null) {
      return getLicenceWorkProgramActivitiesViewsForGivenTerm(scheduleState.currentTerm());
    }

    return List.of();
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
        .map(activity -> createWorkProgrammeActivityView(activity, statusByRef.get(activity.getEventReference())))
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
