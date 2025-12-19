package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class WorkProgrammeActivityService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;

  public WorkProgrammeActivityService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
  }

  public WorkProgrammeActivity getWorkProgrammeActivityByIdOrThrow(UUID id) {
    return workProgrammeActivityRepository.findById(id)
          .orElseThrow(() -> new LmsEntityNotFoundException("WorkProgrammeActivity not found", id.toString()));
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivities(LicenceScheduleDetail licenceScheduleDetail) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndStatus(
        licenceScheduleDetail,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByTermAndDateOption(
      LicenceScheduleTerm licenceScheduleTerm,
      WorkProgrammeActivityDateOption dateOption
  ) {
    return workProgrammeActivityRepository.findAllByLicenceScheduleTermAndDateOptionAndStatus(
        licenceScheduleTerm,
        dateOption,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<WorkProgrammeActivity> getActiveWorkProgrammeActivitiesByPhaseAndDateOption(
      LicenceSchedulePhase licenceSchedulePhase,
      WorkProgrammeActivityDateOption dateOption
  ) {
    return workProgrammeActivityRepository.findAllByLicenceSchedulePhaseAndDateOptionAndStatus(
        licenceSchedulePhase,
        dateOption,
        LicenceScheduleEventStatus.ACTIVE
    );
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
    return workProgrammeActivityRepository.findAllByLicenceScheduleDetailAndDueDateBetweenAndStatus(
        licenceScheduleDetail,
        from,
        to,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  @Transactional
  public void deleteWorkProgrammeActivity(WorkProgrammeActivity workProgrammeActivity) {
    workProgrammeActivity.setStatus(LicenceScheduleEventStatus.DELETED);
    workProgrammeActivityRepository.save(workProgrammeActivity);
  }
}