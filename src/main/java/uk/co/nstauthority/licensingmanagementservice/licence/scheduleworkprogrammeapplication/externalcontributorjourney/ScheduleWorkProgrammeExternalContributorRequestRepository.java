package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.externalcontributorjourney;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;

@Repository
public interface ScheduleWorkProgrammeExternalContributorRequestRepository
    extends JpaRepository<ScheduleWorkProgrammeExternalContributorRequest, UUID>, NotDuplicationSource {

  Optional<ScheduleWorkProgrammeExternalContributorRequest> findByScheduleWorkProgrammeApplication(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication
  );
}
