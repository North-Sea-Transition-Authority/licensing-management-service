package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceReminderMissingContactRepository
    extends JpaRepository<LicenceReminderMissingContact, UUID>, NotDuplicationSource {

  List<LicenceReminderMissingContact> findAllByLicenceIn(Collection<Licence> licences);
}
