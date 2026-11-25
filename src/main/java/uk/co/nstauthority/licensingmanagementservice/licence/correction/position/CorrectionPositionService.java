package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Service
public class CorrectionPositionService {

  private final LicencePositionService licencePositionService;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  CorrectionPositionService(
      LicencePositionService licencePositionService,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  public List<CorrectionPosition> getCorrectionPositions(LicenceCorrection licenceCorrection) {
    var licence = licenceCorrection.getLicence();
    var correctionPositions = new ArrayList<CorrectionPosition>();

    for (var executedLicencePosition : licencePositionService.getExecutedChronologicalLicencePositions(licence)) {
      correctionPositions.add(new CorrectionPosition.Executed(licenceCorrection, executedLicencePosition));
    }

    var addedLicencePositions = licencePositionStagedPayloadService.getAddedLicencePositionStagedPayloads(licenceCorrection);
    for (var addedLicencePosition : addedLicencePositions) {
      correctionPositions.add(new CorrectionPosition.Staged(addedLicencePosition));
    }

    Collections.sort(correctionPositions);
    return correctionPositions;
  }

  /**
   * The position with the given id on the correction's timeline: a position the correction adds, or an executed
   * position on the correction's licence. Empty when the id is neither.
   */
  public Optional<CorrectionPosition> findCorrectionPosition(LicenceCorrection licenceCorrection, UUID positionId) {
    var stagedPosition = licencePositionStagedPayloadService
        .findFirstAddedPositionStagedPayload(licenceCorrection, positionId)
        .<CorrectionPosition>map(CorrectionPosition.Staged::new);

    if (stagedPosition.isPresent()) {
      return stagedPosition;
    }

    return licencePositionService.findById(positionId)
        .filter(licencePosition -> licencePosition.getLicence().getId()
            .equals(licenceCorrection.getLicence().getId()))
        .map(licencePosition -> new CorrectionPosition.Executed(licenceCorrection, licencePosition));
  }

}
