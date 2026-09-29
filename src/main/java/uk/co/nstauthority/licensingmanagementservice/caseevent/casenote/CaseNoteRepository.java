package uk.co.nstauthority.licensingmanagementservice.caseevent.casenote;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, UUID>, NotDuplicationSource {
}
