package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;

@Repository
public interface LicenceScheduleSupportingInformationRepository
    extends JpaRepository<LicenceScheduleSupportingInformation, UUID>, NotDuplicationSource {

  Optional<LicenceScheduleSupportingInformation> findByScheduleWorkProgrammeApplicationDetails(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail);

}