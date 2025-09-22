package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Repository
public interface ScheduleWorkProgrammeApplicationRepository extends JpaRepository<ScheduleWorkProgrammeApplication, UUID> {
  Optional<ScheduleWorkProgrammeApplication> getScheduleWorkProgrammeApplicationByLicenceScheduleDetail(
      LicenceScheduleDetail licenceScheduleDetail);
}
