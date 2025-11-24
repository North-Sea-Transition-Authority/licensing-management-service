package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface WorkProgrammeActivityRepository extends JpaRepository<WorkProgrammeActivity, UUID> {

  List<WorkProgrammeActivity> findWorkProgrammeActivitiesByLicenceScheduleDetail(
      LicenceScheduleDetail licenceScheduleDetail
  );

  boolean existsByIdAndDateOption(UUID id, WorkProgrammeActivityDateOption dateOption);

  Optional<WorkProgrammeActivity> findWorkProgrammeActivityById(UUID id);
}