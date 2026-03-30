package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkProgrammeActivityStatusRepository extends JpaRepository<WorkProgrammeActivityStatus, UUID> {

  List<WorkProgrammeActivityStatus> findAllByActivityEventReference(UUID eventReference);

}
