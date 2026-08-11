package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

@Service
public class PartialSurrenderCorrectionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public PartialSurrenderCorrectionService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  public Optional<PartialSurrenderOperation> getCommittedPartialSurrender(
      LicencePositionCorrection licencePositionCorrection
  ) {
    return licencePositionCorrectionService.getAddOperationsOfType(
            licencePositionCorrection.getPayload().changes(), PartialSurrenderOperation.class)
        .stream()
        .findFirst();
  }

  public Optional<PartialSurrenderOperation> getCommittedPartialSurrenderForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    return licencePositionCorrectionService.findUpdatePositionCorrection(licenceCorrection, licencePosition)
        .flatMap(this::getCommittedPartialSurrender);
  }

  @Transactional
  public void commitPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    applyPartialSurrender(licencePositionCorrection, operation);
  }

  @Transactional
  public void commitPartialSurrenderForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      PartialSurrenderOperation operation
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    applyPartialSurrender(positionCorrection, operation);
  }

  public boolean hasStagedPartialSurrender(LicencePositionCorrection licencePositionCorrection) {
    return getCommittedPartialSurrender(licencePositionCorrection).isPresent();
  }

  private void applyPartialSurrender(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation operation
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection, PartialSurrenderOperation.class, List.of(operation));
  }
}
