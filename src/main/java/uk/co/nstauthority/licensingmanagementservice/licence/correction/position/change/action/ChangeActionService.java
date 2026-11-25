package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

@Service
public class ChangeActionService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final Map<Class<?>, ChangeActionHandler> handlersByType;

  ChangeActionService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      List<ChangeActionHandler> changeActionHandlers
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.handlersByType = changeActionHandlers.stream()
        .collect(Collectors.toMap(AopUtils::getTargetClass, Function.identity()));
  }

  public VisibleLicenceOperation getOperationOfLiveChange(LicencePositionChange liveChange) {
    return firstVisibleOperation(liveChange.getId().toString(), LicencePositionChange.operationsOf(liveChange));
  }

  public VisibleLicenceOperation getOperationOfStagedChange(LicencePositionStagedPayload positionStagedPayload, String changeId) {
    var stagedChange = licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId);
    return firstVisibleOperation(changeId, licencePositionStagedPayloadService.resolveStagedChangeOperations(stagedChange));
  }

  public ChangeActionHandler getHandler(VisibleLicenceOperation operation) {
    var handler = handlersByType.get(operation.changeActionHandlerType());

    if (handler == null) {
      throw new IllegalStateException("%s names %s as its change action handler, which is not a bean".formatted(
          operation.getClass().getSimpleName(),
          operation.changeActionHandlerType().getSimpleName()
      ));
    }

    return handler;
  }

  public ChangeAction getUndoAction(LicencePositionStagedPayload positionStagedPayload, String changeId) {
    if (licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId) instanceof RemoveChange) {
      return ChangeAction.REINSTATE;
    }

    return ChangeAction.UNDO;
  }

  @Transactional
  public void removeLiveChange(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  ) {
    var operation = getOperationOfLiveChange(liveChange);

    if (operation.atMostOnePerTransaction()) {
      licencePositionStagedPayloadService.dropStagedChangesOf(licenceCorrection, licencePosition, operation.getClass());
    }

    licencePositionStagedPayloadService.stageRemovalOfExecutedChange(
        licenceCorrection,
        licencePosition,
        liveChange.getId().toString()
    );
  }

  @Transactional
  public void undoStagedChange(LicencePositionStagedPayload positionStagedPayload, String changeId) {
    var stagedChange = licencePositionStagedPayloadService.getStagedChangeOrThrow(positionStagedPayload, changeId);
    var operation = firstVisibleOperation(
        changeId,
        licencePositionStagedPayloadService.resolveStagedChangeOperations(stagedChange)
    );

    getHandler(operation).beforeDroppingStagedChange(positionStagedPayload, stagedChange);

    licencePositionStagedPayloadService.dropStagedChange(positionStagedPayload, changeId);
  }

  // TODO: LMS2-499 Update changes to store a single operation rather than allowing a list
  private static VisibleLicenceOperation firstVisibleOperation(String changeId, List<LicenceOperation> operations) {
    var operation = operations.stream()
        .filter(VisibleLicenceOperation.class::isInstance)
        .map(VisibleLicenceOperation.class::cast)
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No visible operation on change %s".formatted(changeId)
        ));

    if (operation.changeActionHandlerType() == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "%s on change %s can't be removed, undone or reinstated".formatted(operation.displayName(), changeId)
      );
    }

    return operation;
  }
}
