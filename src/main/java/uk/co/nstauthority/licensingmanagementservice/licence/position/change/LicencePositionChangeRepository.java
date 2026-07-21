package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

@Repository
public interface LicencePositionChangeRepository extends JpaRepository<LicencePositionChange, UUID>, NotDuplicationSource {

  List<LicencePositionChange> findByLicencePositionIn(Collection<LicencePosition> licencePositions);
}
