package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class WorkProgrammeActivityService {

  private final WorkProgrammeActivityRepository workProgrammeActivityRepository;

  public WorkProgrammeActivityService(
      WorkProgrammeActivityRepository workProgrammeActivityRepository
  ) {
    this.workProgrammeActivityRepository = workProgrammeActivityRepository;
  }

  public WorkProgrammeActivity findWorkProgrammeActivityByIdOrThrow(UUID id) {
    return workProgrammeActivityRepository.findWorkProgrammeActivityById(id)
          .orElseThrow(() -> new LmsEntityNotFoundException("WorkProgrammeActivity not found", id.toString()));
  }

  public List<WorkProgrammeActivity> getWorkProgrammeActivities(LicenceScheduleDetail licenceScheduleDetail) {
    return workProgrammeActivityRepository.findWorkProgrammeActivitiesByLicenceScheduleDetail(licenceScheduleDetail);
  }

  public boolean isLinkedToFixedDate(UUID workProgrammeActivityId) {
    return workProgrammeActivityRepository.existsByIdAndDateOption(
        workProgrammeActivityId,
        WorkProgrammeActivityDateOption.FIXED_DATE
    );
  }
}