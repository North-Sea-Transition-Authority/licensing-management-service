package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

@Service
public class SubareaChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public SubareaChangeService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  public boolean hasStagedSubareaChange(LicencePositionCorrection licencePositionCorrection) {
    return licencePositionCorrectionService
        .getCommittedChangeOfType(licencePositionCorrection, SubareaOperation.class).isPresent();
  }

  @Transactional
  public void commitSubareaChangeForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOperation operation
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    applySubareaChange(positionCorrection, operation);
  }

  @Transactional
  public void commitSubareaChange(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOperation operation
  ) {
    applySubareaChange(licencePositionCorrection, operation);
  }

  private void applySubareaChange(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOperation operation
  ) {
    licencePositionCorrectionService.replaceAddChangeFor(
        licencePositionCorrection,
        SubareaOperation.class,
        List.of(operation)
    );
  }
}
