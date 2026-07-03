package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, UUID>, NotDuplicationSource {
}
