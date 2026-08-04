package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public final class LicencePositionChangeViewResolver {

  private LicencePositionChangeViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<String, LicencePositionChangeView> getChangeViews(
      UUID currentPositionId,
      List<ChronologicalPosition> chronologicalPositions,
      Map<UUID, LicencePositionState> statesByChronologicalPositionId,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    var previousState = LicencePositionStateResolver.previousState(
        currentPositionId,
        chronologicalPositions,
        statesByChronologicalPositionId
    );

    return chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(currentPositionId))
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .flatMap(change -> change.operations().stream()
            .map(operation -> Map.entry(
                operation.type(),
                toView(operation, change, previousState, organisationNames, urlContext))))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            LicencePositionChangeView::merge));
  }

  private static LicencePositionChangeView toView(
      LicenceOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(administratorChange, change, previousState, organisationNames, urlContext);
      case SetEquityOperation(var transferTo, var equity) ->
          new SetEquityChangeView(
              List.of(new SetEquityRow(organisationNames.getOrDefault(transferTo, ""), equity)),
              change.changeType()
          );
    };
  }

  private static AdministratorChangeView buildAdministratorChange(
      AdministratorOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    var joiningId = operation.operatorId();

    var withdrawingId = previousState.administratorId();
    var withdrawingName = (withdrawingId == null) ? null : organisationNames.get(withdrawingId);

    return new AdministratorChangeView(
        withdrawingName,
        organisationNames.get(joiningId),
        change.changeId(),
        change.changeType(),
        correctChangeUrl(urlContext, change),
        removeChangeUrl(urlContext, change)
    );
  }

  @Nullable
  //TODO: When other change types are added, we should adapt how the correct / remove urls for change views are built
  private static String correctChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    if (urlContext == null) {
      return null;
    }
    if (urlContext.addedPosition()) {
      return ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
          .renderForAddedPosition(urlContext.correctionId(), urlContext.routingId(), null));
    }

    if (LicencePositionChangeType.ADD_CHANGE.equals(change.changeType())) {
      return ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
          .renderForExecutedPosition(urlContext.correctionId(), urlContext.routingId(), null));
    }
    return ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForCorrectingChange(urlContext.correctionId(), urlContext.routingId(), change.changeId(), null));
  }

  @Nullable
  //TODO: When other change types are added, we should adapt how the correct / remove urls for change views are built
  private static String removeChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    if (urlContext == null || urlContext.addedPosition()) {
      return null;
    }
    var changeType = change.changeType();
    if (LicencePositionChangeType.ADD_CHANGE.equals(changeType)
        || LicencePositionChangeType.REMOVE_CHANGE.equals(changeType)) {
      return null;
    }
    return ReverseRouter.route(on(RemoveAdministratorChangeController.class)
        .renderRemoveExecutedAdminChange(urlContext.correctionId(), urlContext.routingId(), change.changeId(), null));
  }
}
