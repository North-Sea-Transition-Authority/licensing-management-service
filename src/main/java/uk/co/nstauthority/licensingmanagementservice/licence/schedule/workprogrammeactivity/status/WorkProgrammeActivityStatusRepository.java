package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface WorkProgrammeActivityStatusRepository
    extends JpaRepository<WorkProgrammeActivityStatus, UUID>, NotDuplicationSource {

  List<WorkProgrammeActivityStatus> findAllByScheduleEvent_OriginalEventId(UUID originalEventId);

  List<WorkProgrammeActivityStatus> findAllByScheduleEvent_OriginalEventIdIn(Collection<UUID> originalEventIds);
}
