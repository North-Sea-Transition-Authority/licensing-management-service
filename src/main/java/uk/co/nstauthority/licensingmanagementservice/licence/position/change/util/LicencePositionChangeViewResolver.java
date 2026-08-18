package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.RemoveEquityChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeHoldingView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public final class LicencePositionChangeViewResolver {

  private static final String NOT_AVAILABLE = "Not available";

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
    var stateBeforeCurrentPosition = resolvedStates.previousState(currentPositionId);

    var currentPosition = chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(currentPositionId))
        .toList();

    var currentPositionDate = getCurrentPositionDate(currentPosition);

    var currentPositionChanges = currentPosition.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .toList();

    var changeViews = new HashMap<String, LicencePositionChangeView>();
    var stateBeforeChange = stateBeforeCurrentPosition;

    for (var change : currentPositionChanges) {
      for (var operation : change.operations()) {
        changeViews.merge(
            operation.type(),
            toView(operation, change, stateBeforeChange,
                currentPositionDate,
                organisationNames,
                featureNames,
                urlContext
            ),
            LicencePositionChangeView::merge
        );
      }
      stateBeforeChange = LicencePositionStateResolver.applyChange(stateBeforeChange, change);
    }

    return changeViews;
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
      case SetEquityOperation setEquityOperation ->
          buildSetEquityChangeView(setEquityOperation, change, organisationNames, urlContext);
      case TransferEquityOperation transferEquityOperation ->
          buildTransferEquityChangeView(transferEquityOperation, change, previousState, organisationNames, urlContext);
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

  private static SetEquityChangeView buildSetEquityChangeView(
      SetEquityOperation operation,
      PositionChange change,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    return new SetEquityChangeView(
        List.of(new SetEquityRow(organisationNames.getOrDefault(operation.transferTo(), NOT_AVAILABLE), operation.equity())),
        change.changeType(),
        correctEquityChangeUrl(
            urlContext,
            change,
            ctx -> ReverseRouter.route(on(LicencePositionSetEquityController.class)
                .renderSummaryForAddedPosition(ctx.correctionId(), ctx.routingId(), null)),
            ctx -> ReverseRouter.route(on(LicencePositionSetEquityController.class)
                .renderSummaryForExecutedPosition(ctx.correctionId(), ctx.routingId(), null))),
        undoEquityChangeUrl(urlContext, change)
    );
  }

  private static TransferEquityChangeView buildTransferEquityChangeView(
      TransferEquityOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    var transferFrom = operation.transferFrom();
    var transferTo = operation.transferTo();

    var startingEquityByOrganisationId = previousState.equityByOrganisationId();
    var resultingEquityByOrganisationId =
        LicencePositionStateResolver.applyTransferEquity(previousState, operation).equityByOrganisationId();

    return new TransferEquityChangeView(
        List.of(new TransferEquityChangeHoldingView(
            organisationNames.getOrDefault(transferFrom, NOT_AVAILABLE),
            startingEquityByOrganisationId.getOrDefault(transferFrom, BigDecimal.ZERO),
            resultingEquityByOrganisationId.getOrDefault(transferFrom, BigDecimal.ZERO),
            organisationNames.getOrDefault(transferTo, NOT_AVAILABLE),
            startingEquityByOrganisationId.getOrDefault(transferTo, BigDecimal.ZERO),
            resultingEquityByOrganisationId.getOrDefault(transferTo, BigDecimal.ZERO),
            operation.equity(),
            operation.retainBeneficialInterest()
        )),
        change.changeType(),
        correctEquityChangeUrl(
            urlContext,
            change,
            ctx -> ReverseRouter.route(on(LicencePositionTransferEquityController.class)
                .renderSummaryForAddedPosition(ctx.correctionId(), ctx.routingId(), null)),
            ctx -> ReverseRouter.route(on(LicencePositionTransferEquityController.class)
                .renderSummaryForExecutedPosition(ctx.correctionId(), ctx.routingId(), null))
        ),
        undoEquityChangeUrl(urlContext, change)
    );
  }

  @Nullable
  private static String correctEquityChangeUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      Function<PositionChangeUrlContext, String> addedPositionSummaryUrl,
      Function<PositionChangeUrlContext, String> executedPositionSummaryUrl
  ) {
    if (urlContext == null) {
      return null;
    }

    var changeType = change.changeType();
    var addedOrCorrectedInThisCorrection = LicencePositionChangeType.ADD_CHANGE.equals(changeType)
        || LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS.equals(changeType);
    if (!addedOrCorrectedInThisCorrection) {
      return null;
    }

    return urlContext.addedPosition()
        ? addedPositionSummaryUrl.apply(urlContext)
        : executedPositionSummaryUrl.apply(urlContext);
  }

  @Nullable
  private static String undoEquityChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    if (urlContext == null || change.changeType() == null) {
      return null;
    }
    return ReverseRouter.route(on(RemoveEquityChangeController.class)
        .renderUndoEquityChange(urlContext.correctionId(), change.changeId(), null));
  }

  @Nullable
  //TODO When other change types are added, we should adapt how the correct urls for change views are built
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
