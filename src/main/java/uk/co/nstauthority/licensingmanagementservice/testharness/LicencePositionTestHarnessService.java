package uk.co.nstauthority.licensingmanagementservice.testharness;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Service
public class LicencePositionTestHarnessService {

  private final LicencePositionRepository licencePositionRepository;
  private final LicencePositionChangeService licencePositionChangeService;

  public LicencePositionTestHarnessService(
      LicencePositionRepository licencePositionRepository,
      LicencePositionChangeService licencePositionChangeService
  ) {
    this.licencePositionRepository = licencePositionRepository;
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Transactional
  public void clearPositionsForLicence(Licence licence) {
    var licencePositions = licencePositionRepository.findByLicence(licence);

    if (licencePositions.isEmpty()) {
      return;
    }

    licencePositionChangeService.deleteForPositions(licencePositions);
    licencePositionRepository.deleteAll(licencePositions);
  }
}
