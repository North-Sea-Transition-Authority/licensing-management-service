package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.SetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil;

@Service
public class EquityChangeService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final TransferEquityCorrectionService transferEquityCorrectionService;
  private final SetEquityCorrectionService setEquityCorrectionService;
  private final LicencePositionChangeService licencePositionChangeService;

  public EquityChangeService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      TransferEquityCorrectionService transferEquityCorrectionService,
      SetEquityCorrectionService setEquityCorrectionService, LicencePositionChangeService licencePositionChangeService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.transferEquityCorrectionService = transferEquityCorrectionService;
    this.setEquityCorrectionService = setEquityCorrectionService;
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Transactional
  public void removeExistingEquityChange(
      LicencePosition licencePosition,
      LicenceCorrection licenceCorrection,
      String changeId
  ) {
    licencePositionCorrectionService.stageRemovalOfExecutedChange(licenceCorrection, licencePosition, changeId);
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

    var remainingChanges = LicencePositionChangeType.removeChangesById(payload.changes(), changeId);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.UPDATE_POSITION
        && remainingChanges.isEmpty()
        && LicencePositionChangeUtil.positionDateAndOrderUnchanged(positionCorrection)) {
      licencePositionCorrectionService.delete(positionCorrection);
      return;
    }

    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, remainingChanges));
    licencePositionCorrectionService.save(positionCorrection);
  }

  public EquityChangeContext getExecutedEquityChangeContext(String changeId) {
    var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId));
    return buildContext(LicencePositionChange.operationsOf(liveChange));
  }

  public EquityChangeContext getEquityChangeContext(LicenceCorrection licenceCorrection, String changeId) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(licenceCorrection, changeId);

    return buildContext(resolveOperations(findChange(positionCorrection, changeId)));
  }

  private EquityChangeContext buildContext(List<LicenceOperation> operations) {
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

    return new EquityChangeContext(setEquityRows, transferEquityRows);
  }

  private LicencePositionChangeType findChange(LicencePositionCorrection positionCorrection, String changeId) {
    return positionCorrection.getPayload().changes().stream()
        .filter(not(LicencePositionChangeType::isUpdateChangeOrder))
        .filter(change -> changeId.equals(change.changeId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No change with id %s found in position correction %s"
                .formatted(changeId, positionCorrection.getId())));
  }

  private boolean isEquityChange(LicencePositionChangeType change) {
    return containsEquityOperation(resolveOperations(change));
  }

  private List<LicenceOperation> resolveOperations(LicencePositionChangeType change) {
    if (change instanceof RemoveChange(String changeId)) {
      var liveChange = licencePositionChangeService.getByIdOrThrow(UUID.fromString(changeId));
      return LicencePositionChange.operationsOf(liveChange);
    }
    return LicencePositionChangeType.operationsOf(change);
  }

  private boolean containsEquityOperation(List<LicenceOperation> operations) {
    return operations.stream()
        .anyMatch(operation -> operation instanceof SetEquityOperation || operation instanceof TransferEquityOperation);
  }
}