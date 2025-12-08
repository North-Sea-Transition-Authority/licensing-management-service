package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
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
}