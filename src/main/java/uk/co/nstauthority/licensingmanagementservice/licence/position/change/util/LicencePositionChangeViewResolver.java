package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public final class LicencePositionChangeViewResolver {

  private LicencePositionChangeViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static Map<String, LicencePositionChangeView> getChangeViews(
      UUID currentPositionId,
      List<ChronologicalPosition> chronologicalPositions,
      ResolvedStates resolvedStates,
      Map<Integer, String> organisationNames,
      Map<UUID, String> featureNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    var previousState = resolvedStates.previousState(currentPositionId);

    var currentPosition = chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(currentPositionId))
        .toList();

    var currentPositionDate = getCurrentPositionDate(currentPosition);

    return currentPosition.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .flatMap(change -> change.operations().stream()
            .map(operation -> Map.entry(
                operation.type(),
                toView(
                    operation,
                    change,
                    previousState,
                    currentPositionDate,
                    organisationNames,
                    featureNames,
                    urlContext
                ))))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            LicencePositionChangeView::merge));
  }

  @Nullable
  private static LocalDate getCurrentPositionDate(List<ChronologicalPosition> currentPosition) {
    return currentPosition.stream()
        .map(ChronologicalPosition::date)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static LicencePositionChangeView toView(
      LicenceOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      Map<Integer, String> organisationNames,
      Map<UUID, String> featureNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(administratorChange, change, previousState, organisationNames, urlContext);
      case SetEquityOperation(var transferTo, var equity) -> new SetEquityChangeView(
          List.of(new SetEquityRow(organisationNames.getOrDefault(transferTo, ""), equity)),
          change.changeType()
      );
      case TransferEquityOperation(var transferFrom, var transferTo, var equity, var remainBeneficialInterest) ->
          new TransferEquityChangeView(
              List.of(new TransferEquityHoldingView(
                  organisationNames.getOrDefault(transferFrom, ""),
                  organisationNames.getOrDefault(transferTo, ""),
                  equity,
                  remainBeneficialInterest)),
              change.changeType()
          );
      case PartialSurrenderOperation partialSurrenderOperation ->
          buildPartialSurrenderChange(partialSurrenderOperation, change, currentPositionDate, featureNames);
    };
  }

  private static PartialSurrenderChangeView buildPartialSurrenderChange(
      PartialSurrenderOperation operation,
      PositionChange change,
      @Nullable LocalDate currentPositionDate,
      Map<UUID, String> featureNames
  ) {
    var surrenderDate = operation.surrenderDate() != null ? operation.surrenderDate() : currentPositionDate;

    var blockLabels = operation.featureIds()
        .stream()
        .map(featureId -> featureNames.getOrDefault(featureId, ""))
        .toList();

    return new PartialSurrenderChangeView(
        surrenderDate == null ? null : DateUtil.formatLongDate(surrenderDate),
        blockLabels,
        change.changeType()
    );
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
        removeChangeUrl(urlContext, change),
        undoChangeUrl(urlContext, change)
    );
  }

  @Nullable
  //TODO LMS2-132: When other change types are added, we should adapt how the correct urls for change views are built
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
    if (LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType())) {
      return null;
    }
    return ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
        .renderForCorrectingChange(urlContext.correctionId(), urlContext.routingId(), change.changeId(), null));
  }

  @Nullable
  //TODO LMS2-133: When other change types are added, we should adapt how the remove urls for change views are built
  private static String removeChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    if (urlContext == null || urlContext.addedPosition()) {
      return null;
    }

    if (change.changeType() != null) {
      return null;
    }
    return ReverseRouter.route(on(RemoveAdministratorChangeController.class)
        .renderRemoveExecutedAdminChange(urlContext.correctionId(), urlContext.routingId(), change.changeId(), null));
  }

  @Nullable
  //TODO LMS2-134: When other change types are added, we should adapt how the undo urls for change  views are built
  private static String undoChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    if (urlContext == null || change.changeType() == null) {
      return null;
    }
    return ReverseRouter.route(on(RemoveAdministratorChangeController.class)
        .renderUndoAdminChange(urlContext.correctionId(), change.changeId(), null));
  }
}
