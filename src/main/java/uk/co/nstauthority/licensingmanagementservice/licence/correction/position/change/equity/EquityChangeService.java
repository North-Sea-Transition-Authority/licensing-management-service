package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil;

@Service
public class EquityChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final TransferEquityCorrectionService transferEquityCorrectionService;
  private final SetEquityCorrectionService setEquityCorrectionService;

  public EquityChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      TransferEquityCorrectionService transferEquityCorrectionService,
      SetEquityCorrectionService setEquityCorrectionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
    this.setEquityCorrectionService = setEquityCorrectionService;
  }

  @Transactional
  public void undoEquityChange(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var payload = positionCorrection.getPayload();
    var changeToUndo = findChange(positionCorrection, changeId);

    if (!isEquityChange(changeToUndo)) {
      throw new IllegalStateException(
          "Change with id %s is not a beneficial interest change".formatted(changeId));
    }

    var remainingChanges = LicencePositionChangeUtil.removeChangeById(payload.changes(), changeId);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.UPDATE_POSITION
        && remainingChanges.isEmpty()
        && LicencePositionChangeUtil.positionDateAndOrderUnchanged(positionCorrection)) {
      licencePositionCorrectionService.delete(positionCorrection);
      return;
    }

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, remainingChanges));
    licencePositionCorrectionService.save(positionCorrection);
  }

  public EquityChangeUndoView getEquityChangeUndoView(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    var operations = LicencePositionChangeType.operationsOf(findChange(positionCorrection, changeId));

    var setEquityRows = setEquityCorrectionService.getSetEquityViews(
        operations.stream()
            .filter(SetEquityOperation.class::isInstance)
            .map(SetEquityOperation.class::cast)
            .toList());

    var transferEquityRows = transferEquityCorrectionService.getTransferEquityViews(
        operations.stream()
            .filter(TransferEquityOperation.class::isInstance)
            .map(TransferEquityOperation.class::cast)
            .toList());

    return new EquityChangeUndoView(setEquityRows, transferEquityRows);
  }

  private LicencePositionChangeType findChange(LicencePositionCorrection positionCorrection, String changeId) {
    return positionCorrection.getPayload().changes().stream()
        .filter(change -> changeId.equals(change.changeId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No change with id %s found in position correction %s"
                .formatted(changeId, positionCorrection.getId())));
  }

  private boolean isEquityChange(LicencePositionChangeType change) {
    return containsEquityOperation(LicencePositionChangeType.operationsOf(change));
  }

  private boolean containsEquityOperation(List<LicenceOperation> operations) {
    return operations.stream()
        .anyMatch(operation -> operation instanceof SetEquityOperation || operation instanceof TransferEquityOperation);
  }
}