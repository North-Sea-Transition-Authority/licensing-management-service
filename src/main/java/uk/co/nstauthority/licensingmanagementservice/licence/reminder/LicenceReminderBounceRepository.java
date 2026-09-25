package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface LicenceReminderBounceRepository
    extends JpaRepository<LicenceReminderBounce, UUID>, NotDuplicationSource {
}
