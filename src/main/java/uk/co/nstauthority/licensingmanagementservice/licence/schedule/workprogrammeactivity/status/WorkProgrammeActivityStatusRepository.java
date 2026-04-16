package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface WorkProgrammeActivityStatusRepository
    extends JpaRepository<WorkProgrammeActivityStatus, UUID>, NotDuplicationSource {

  List<WorkProgrammeActivityStatus> findAllByActivityEventReference(UUID eventReference);

  List<WorkProgrammeActivityStatus> findAllByActivityEventReferenceIn(List<UUID> eventReferences);
}
