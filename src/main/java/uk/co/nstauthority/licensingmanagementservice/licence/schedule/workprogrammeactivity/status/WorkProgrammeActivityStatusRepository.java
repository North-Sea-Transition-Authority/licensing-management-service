package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;

@Repository
public interface WorkProgrammeActivityStatusRepository
    extends JpaRepository<WorkProgrammeActivityStatus, UUID>, NotDuplicationSource {

  List<WorkProgrammeActivityStatus> findAllByEventReference(EventReference eventReference);

  List<WorkProgrammeActivityStatus> findAllByEventReferenceIn(List<EventReference> eventReferences);
}
