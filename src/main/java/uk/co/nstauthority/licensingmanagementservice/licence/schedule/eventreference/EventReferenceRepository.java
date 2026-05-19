package uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface EventReferenceRepository extends JpaRepository<EventReference, UUID>, NotDuplicationSource {
}
