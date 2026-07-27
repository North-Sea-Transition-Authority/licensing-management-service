package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

@Service
public class LicencePositionChangeService {

  private final LicencePositionChangeRepository licencePositionChangeRepository;

  public LicencePositionChangeService(LicencePositionChangeRepository licencePositionChangeRepository) {
    this.licencePositionChangeRepository = licencePositionChangeRepository;
  }

  public List<LicencePositionChange> findByLicencePositionIn(Collection<LicencePosition> licencePositions) {
    return licencePositionChangeRepository.findByLicencePositionIn(licencePositions);
  }

  public Optional<LicencePositionChange> findById(UUID id) {
    return licencePositionChangeRepository.findById(id);
  }

  @Transactional
  public LicencePositionChange createLicencePositionChange(
      LicencePosition licencePosition,
      List<LicenceOperation> operations,
      int changeOrder,
      LicencePositionChangeStatus status
  ) {
    var licencePositionChange = new LicencePositionChange();
    licencePositionChange.setLicencePosition(licencePosition);
    licencePositionChange.setOperations(operations);
    licencePositionChange.setChangeOrder(changeOrder);
    licencePositionChange.setStatus(status);

    return licencePositionChangeRepository.save(licencePositionChange);
  }

  @Transactional
  public void deleteForPositions(Collection<LicencePosition> licencePositions) {
    if (licencePositions.isEmpty()) {
      return;
    }

    var licencePositionChanges = licencePositionChangeRepository.findByLicencePositionIn(licencePositions);
    licencePositionChangeRepository.deleteAll(licencePositionChanges);
  }
}
