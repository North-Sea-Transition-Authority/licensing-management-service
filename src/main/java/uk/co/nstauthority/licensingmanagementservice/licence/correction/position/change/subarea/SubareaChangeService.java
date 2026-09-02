package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

@Service
public class SubareaChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public SubareaChangeService(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @Transactional
  public void commitSubareaChangeForExecutedPosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      SubareaOperation operation
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getOrBuildUpdatePositionCorrection(licenceCorrection, licencePosition);

    replaceAddSubareaChangeForBlock(positionCorrection, operation);
  }

  @Transactional
  public void commitSubareaChange(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOperation operation
  ) {
    replaceAddSubareaChangeForBlock(licencePositionCorrection, operation);
  }

  private void replaceAddSubareaChangeForBlock(
      LicencePositionCorrection licencePositionCorrection,
      SubareaOperation operation
  ) {
    var payload = licencePositionCorrection.getPayload();

    var changes = payload.changes().stream()
        .filter(change -> !isAddSubareaChangeForBlock(change, operation.featureId()))
        .collect(Collectors.toCollection(ArrayList::new));

    changes.add(AddChange.buildOperationsChange(List.of(operation), licencePositionCorrectionService.nextChangeOrder(changes)));

    licencePositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, changes));
    licencePositionCorrectionService.save(licencePositionCorrection);
  }

  private boolean isAddSubareaChangeForBlock(LicencePositionChangeType change, UUID blockFeatureId) {
    return licencePositionCorrectionService.getAddOperationsOfType(List.of(change), SubareaOperation.class).stream()
        .anyMatch(op -> blockFeatureId.equals(op.featureId()));
  }
}
