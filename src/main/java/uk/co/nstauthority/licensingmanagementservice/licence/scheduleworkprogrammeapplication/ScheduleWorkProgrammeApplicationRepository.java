package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleWorkProgrammeApplicationRepository extends JpaRepository<ScheduleWorkProgrammeApplication, UUID> {
}
