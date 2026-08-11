package uk.co.nstauthority.licensingmanagementservice.licence.status;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceStatusRepository
    extends JpaRepository<LicenceStatus, UUID>, NotDuplicationSource {

  List<LicenceStatus> findAllByLicence(Licence licence);

  List<LicenceStatus> findAllByLicence_IdIn(Collection<Integer> licenceIds);
}
