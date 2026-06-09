package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Repository
public interface LicenceCorrectionRepository extends JpaRepository<LicenceCorrection, UUID>, NotDuplicationSource {

  boolean existsByLicenceAndStatus(Licence licence, LicenceCorrectionStatus status);
}
