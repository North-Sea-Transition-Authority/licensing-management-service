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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.RemoveChange;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

@Service
public class ChangeActionService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final Map<Class<?>, ChangeActionHandler> handlersByType;

  ChangeActionService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      List<ChangeActionHandler> changeActionHandlers
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.handlersByType = changeActionHandlers.stream()
        .collect(Collectors.toMap(AopUtils::getTargetClass, Function.identity()));
  }

  public VisibleLicenceOperation getOperationOfLiveChange(LicencePositionChange liveChange) {
    return firstVisibleOperation(liveChange.getId().toString(), LicencePositionChange.operationsOf(liveChange));
  }

  public VisibleLicenceOperation getOperationOfStagedChange(LicencePositionCorrection positionCorrection, String changeId) {
    var stagedChange = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);
    return firstVisibleOperation(changeId, licencePositionCorrectionService.resolveStagedChangeOperations(stagedChange));
  }

  /**
   * The handler the operation names for itself.
   *
   * @throws IllegalStateException when the handler the operation names is not a bean
   */
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

  /**
   * Undoing a staged removal reinstates the live change; undoing anything else undoes this correction's change.
   */
  public ChangeAction getUndoAction(LicencePositionCorrection positionCorrection, String changeId) {
    if (licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId) instanceof RemoveChange) {
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

    if (operation.atMostOnePerPosition()) {
      licencePositionCorrectionService.dropStagedChangesOf(licenceCorrection, licencePosition, operation.getClass());
    }

    licencePositionCorrectionService.stageRemovalOfExecutedChange(
        licenceCorrection,
        licencePosition,
        liveChange.getId().toString()
    );
  }

  @Transactional
  public void undoStagedChange(LicencePositionCorrection positionCorrection, String changeId) {
    var stagedChange = licencePositionCorrectionService.getStagedChangeOrThrow(positionCorrection, changeId);
    var operation = firstVisibleOperation(
        changeId,
        licencePositionCorrectionService.resolveStagedChangeOperations(stagedChange)
    );

    getHandler(operation).beforeDroppingStagedChange(positionCorrection, stagedChange);

    licencePositionCorrectionService.dropStagedChange(positionCorrection, changeId);
  }

  /**
   * A change is acted on as the first of its operations shown on a position.
   *
   * @throws ResponseStatusException with {@link HttpStatus#NOT_FOUND} when the change has no visible operation, or its
   *                                 first one has no change action handler, so no action can be taken on it
   */
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
