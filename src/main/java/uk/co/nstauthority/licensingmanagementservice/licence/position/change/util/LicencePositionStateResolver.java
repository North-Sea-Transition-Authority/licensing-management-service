package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

public final class LicencePositionStateResolver {

  private LicencePositionStateResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<UUID, LicencePositionState> resolveStatesByChronologicalPositionId(
      List<ChronologicalPosition> chronologicalPositions
  ) {
    var statesByChronologicalPositionId = new HashMap<UUID, LicencePositionState>();

    var currentState = LicencePositionState.EMPTY;
    for (var chronologicalPosition : chronologicalPositions) {
      currentState = applyChanges(currentState, chronologicalPosition);
      statesByChronologicalPositionId.put(chronologicalPosition.id(), currentState);
    }

    return statesByChronologicalPositionId;
  }

  public static LicencePositionState previousState(
      UUID currentLicencePositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<UUID, LicencePositionState> statesByChronologicalPositionId
  ) {
    var previousState = LicencePositionState.EMPTY;
    for (var chronologicalPosition : chronologicalPositions) {
      if (chronologicalPosition.id().equals(currentLicencePositionId)) {
        return previousState;
      }
      previousState = statesByChronologicalPositionId.getOrDefault(chronologicalPosition.id(), LicencePositionState.EMPTY);
    }
    return previousState;
  }

  private static LicencePositionState applyChanges(
      LicencePositionState licencePositionState,
      ChronologicalPosition chronologicalPosition
  ) {
    var currentState = licencePositionState;
    var changes = chronologicalPosition.changes();

    for (var change : changes) {
      currentState = applyChange(currentState, change);
    }

    return currentState;
  }

  private static LicencePositionState applyChange(LicencePositionState state, PositionChange change) {
    if (Objects.equals(change.changeType(), LicencePositionChangeType.REMOVE_CHANGE)) {
      return state;
    }

    var setEquityOperations = change.operations().stream()
        .filter(SetEquityOperation.class::isInstance)
        .map(SetEquityOperation.class::cast)
        .toList();

    var currentState = state;

    // Set equity operations defines the equity holdings for each organisation
    // which is then used to transfer equity between organisations
    if (!setEquityOperations.isEmpty()) {
      currentState = applySetEquityChange(currentState, setEquityOperations);
    }

    for (var operation : change.operations()) {
      if (operation instanceof SetEquityOperation) {
        continue;
      }
      currentState = applyOperation(currentState, operation);
    }
    return currentState;
  }

  private static LicencePositionState applyOperation(LicencePositionState state, LicenceOperation operation) {
    //TODO extend the switch statement as other operation types are added
    return switch (operation) {
      case AdministratorOperation administratorOperation ->
          state.withAdministratorId(administratorOperation.operatorId());
      case TransferEquityOperation transferEquityOperation ->
          applyTransferEquity(state, transferEquityOperation);
      case SetEquityOperation setEquityOperation -> state;
    };
  }

  private static LicencePositionState applySetEquityChange(
      LicencePositionState state,
      List<SetEquityOperation> setEquityOperations
  ) {
    var equityByOrganisationId = new HashMap<Integer, BigDecimal>();
    for (var setEquityOperation : setEquityOperations) {
      equityByOrganisationId.put(setEquityOperation.transferTo(), setEquityOperation.equity());
    }
    return state.withEquityByOrganisationId(equityByOrganisationId);
  }

  private static LicencePositionState applyTransferEquity(
      LicencePositionState state,
      TransferEquityOperation transferEquityOperation
  ) {
    var equityByOrganisationId = new HashMap<>(state.equityByOrganisationId());

    var transferFrom = transferEquityOperation.transferFrom();
    var transferTo = transferEquityOperation.transferTo();
    var requestedEquity = transferEquityOperation.equity();

    var isAddingEquity = requestedEquity.signum() > 0;
    if (!isAddingEquity) {
      return state.withEquityByOrganisationId(equityByOrganisationId);
    }
    var availableEquity = equityByOrganisationId.getOrDefault(transferFrom, BigDecimal.ZERO).max(BigDecimal.ZERO);
    var transferEquity = requestedEquity.min(availableEquity);
    var remainingEquity = availableEquity.subtract(transferEquity);

    var retainsInterest = Boolean.TRUE.equals(transferEquityOperation.retainBeneficialInterest());

    if (remainingEquity.signum() <= 0 && !retainsInterest) {
      equityByOrganisationId.remove(transferFrom);
    } else {
      equityByOrganisationId.put(transferFrom, remainingEquity);
    }

    equityByOrganisationId.merge(transferTo, transferEquity, BigDecimal::add);

    return state.withEquityByOrganisationId(equityByOrganisationId);
  }
}