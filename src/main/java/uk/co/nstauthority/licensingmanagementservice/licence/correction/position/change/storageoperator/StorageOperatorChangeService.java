package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;

@Service
public class StorageOperatorChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final ChronologicalPositionService chronologicalPositionService;
  private final OrganisationChangeService organisationChangeService;

  StorageOperatorChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      OrganisationChangeService organisationChangeService,
      ChronologicalPositionService chronologicalPositionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.organisationChangeService = organisationChangeService;
    this.chronologicalPositionService = chronologicalPositionService;
  }

  public String getJoiningOperatorForReinstating(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection positionCorrectionToUndo
  ) {
    var licencePositionId = positionCorrectionToUndo.getPositionId();

    var remainingPositionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection)
        .stream()
        .filter(correction -> !correction.getId().equals(positionCorrectionToUndo.getId()))
        .toList();

    var targetPositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        remainingPositionCorrections,
        Set.of(licencePositionId)
    );

    var uncorrectedChangeContext = organisationChangeService
        .getOrganisationChangeContext(licenceCorrection, licencePositionId, StorageOperatorOperation.class, targetPositions);

    return uncorrectedChangeContext.currentName();
  }

}
