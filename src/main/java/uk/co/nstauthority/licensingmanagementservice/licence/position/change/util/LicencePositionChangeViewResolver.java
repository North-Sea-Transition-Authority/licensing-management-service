package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.RemoveEquityChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.RemovePartialSurrenderChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.PartialSurrenderChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SubareaChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeHoldingView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

public final class LicencePositionChangeViewResolver {

  private LicencePositionChangeViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static List<LicencePositionChangeView> getChangeViews(
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

    var canReorder = currentPositionChanges.stream().filter(PositionChange::isOrderable).count() > 1;

    var changeViews = new ArrayList<LicencePositionChangeView>();
    var stateBeforeChange = stateBeforeCurrentPosition;

    for (var change : currentPositionChanges) {
      var correctChangeOrderUrl = canReorder
          ? correctChangeOrderUrl(urlContext, change, currentPositionId)
          : null;
      var stateForChange = stateBeforeChange;

      // Merge a change's operations of the same type into one card, keeping distinct types as separate cards.
      var viewsByType = new LinkedHashMap<String, LicencePositionChangeView>();
      for (var operation : change.operations()) {
        var view = toView(operation, change, stateForChange,
            currentPositionDate,
            organisationNames,
            featureNames,
            urlContext,
            correctChangeOrderUrl
        );
        viewsByType.merge(view.type(), view, LicencePositionChangeView::merge);
      }
      changeViews.addAll(viewsByType.values());

      stateBeforeChange = LicencePositionStateResolver.applyChange(stateBeforeChange, change);
    }

    return changeViews;
  }

  public static Map<UUID, String> getOrderableChangeLabels(
      List<PositionChange> changes,
      Map<UUID, String> featureNames
  ) {
    var labels = new LinkedHashMap<UUID, String>();
    changes.stream()
        .filter(PositionChange::isOrderable)
        .forEach(positionChange -> labels.put(
            UUID.fromString(positionChange.changeId()),
            orderableChangeLabel(positionChange, featureNames))
        );
    return labels;
  }

  private static String orderableChangeLabel(PositionChange change, Map<UUID, String> featureNames) {
    var operation = change.operations().getFirst();
    if (operation instanceof SubareaOperation subarea) {
      return "%s – %s".formatted(operation.displayName(), featureNames.getOrDefault(subarea.featureId(), NOT_AVAILABLE));
    }
    return operation.displayName();
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
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(administratorChange, change, previousState, organisationNames, urlContext,
              correctChangeOrderUrl);
      case SetEquityOperation setEquityOperation ->
          buildSetEquityChangeView(setEquityOperation, change, organisationNames, urlContext, correctChangeOrderUrl);
      case TransferEquityOperation transferEquityOperation ->
          buildTransferEquityChangeView(
              transferEquityOperation,
              change,
              previousState,
              organisationNames,
              urlContext,
              correctChangeOrderUrl
          );
      case PartialSurrenderOperation partialSurrenderOperation ->
          buildPartialSurrenderChange(
              partialSurrenderOperation,
              change,
              currentPositionDate,
              featureNames,
              urlContext,
              correctChangeOrderUrl
          );
      case SubareaOperation subareaOperation ->
          buildSubareaChange(subareaOperation, change, featureNames, correctChangeOrderUrl);
    };
  }

  private static SubareaChangeView buildSubareaChange(
      SubareaOperation operation,
      PositionChange change,
      Map<UUID, String> featureNames,
      @Nullable String correctChangeOrderUrl
  ) {
    return new SubareaChangeView(
        featureNames.getOrDefault(operation.featureId(), NOT_AVAILABLE),
        change.changeType(),
        new ChangeViewUrls(null, null, null, correctChangeOrderUrl)
    );
  }

  private static PartialSurrenderChangeView buildPartialSurrenderChange(
      PartialSurrenderOperation operation,
      PositionChange change,
      @Nullable LocalDate currentPositionDate,
      Map<UUID, String> featureNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl
  ) {
    var surrenderDate = operation.surrenderDate() != null ? operation.surrenderDate() : currentPositionDate;

    var blockRows = operation.featureIds()
        .stream()
        .map(featureId -> new PartialSurrenderChangeView.BlockRow(
            featureNames.getOrDefault(featureId, NOT_AVAILABLE),
            Objects.requireNonNullElse(operation.surrenderTypeDisplayName(featureId), NOT_AVAILABLE)
        ))
        .toList();

    return new PartialSurrenderChangeView(
        surrenderDate == null ? null : DateUtil.formatLongDate(surrenderDate),
        blockRows,
        change.changeType(),
        new ChangeViewUrls(
            partialSurrenderCorrectUrl(urlContext, change),
            removeChangeUrl(urlContext, change,
                ctx -> ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
                    .renderRemoveExecutedPartialSurrender(
                        ctx.correctionId(), ctx.routingId(), change.changeId(), null))),
            null,
            correctChangeOrderUrl
        )
    );
  }

  private static AdministratorChangeView buildAdministratorChange(
      AdministratorOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl
  ) {
    var joiningId = operation.operatorId();

    var withdrawingId = previousState.administratorId();
    var withdrawingName = (withdrawingId == null) ? null : organisationNames.getOrDefault(withdrawingId, NOT_AVAILABLE);

    return new AdministratorChangeView(
        withdrawingName,
        organisationNames.getOrDefault(joiningId, NOT_AVAILABLE),
        change.changeId(),
        change.changeType(),
        new ChangeViewUrls(
            administratorCorrectChangeUrl(urlContext, change),
            removeChangeUrl(urlContext, change,
                ctx -> ReverseRouter.route(on(RemoveAdministratorChangeController.class)
                    .renderRemoveExecutedAdminChange(
                        ctx.correctionId(), ctx.routingId(), change.changeId(), null))),
            undoChangeUrl(urlContext, change),
            correctChangeOrderUrl
        )
    );
  }

  private static SetEquityChangeView buildSetEquityChangeView(
      SetEquityOperation operation,
      PositionChange change,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl
  ) {
    return new SetEquityChangeView(
        List.of(new SetEquityRow(organisationNames.getOrDefault(operation.transferTo(), NOT_AVAILABLE), operation.equity())),
        change.changeType(),
        new ChangeViewUrls(
            correctEquityChangeUrl(
                urlContext,
                change,
                ctx -> ReverseRouter.route(on(LicencePositionSetEquityController.class)
                    .renderSummaryForAddedPosition(ctx.correctionId(), ctx.routingId(), null)),
                ctx -> ReverseRouter.route(on(LicencePositionSetEquityController.class)
                    .renderSummaryForExecutedPosition(ctx.correctionId(), ctx.routingId(), null))),
            removeEquityChangeUrl(urlContext, change),
            undoEquityChangeUrl(urlContext, change),
            correctChangeOrderUrl
        )
    );
  }

  private static TransferEquityChangeView buildTransferEquityChangeView(
      TransferEquityOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl
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
        new ChangeViewUrls(
            correctEquityChangeUrl(
                urlContext,
                change,
                ctx -> ReverseRouter.route(on(LicencePositionTransferEquityController.class)
                    .renderSummaryForAddedPosition(ctx.correctionId(), ctx.routingId(), null)),
                ctx -> ReverseRouter.route(on(LicencePositionTransferEquityController.class)
                    .renderSummaryForExecutedPosition(ctx.correctionId(), ctx.routingId(), null))
            ),
            removeEquityChangeUrl(urlContext, change),
            undoEquityChangeUrl(urlContext, change),
            correctChangeOrderUrl
        )
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
  private static String removeEquityChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    return removeChangeUrl(
        urlContext,
        change,
        ctx -> ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderRemoveExecutedEquityChange(ctx.correctionId(), ctx.routingId(), change.changeId(), null))
    );
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
  private static String correctChangeOrderUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      UUID currentPositionId
  ) {
    if (urlContext == null || !change.isOrderable()) {
      return null;
    }
    return ReverseRouter.route(on(CorrectChangeOrderController.class)
        .renderCorrectChangeOrder(urlContext.correctionId(), currentPositionId, UUID.fromString(change.changeId()), null));
  }

  @Nullable
  private static String administratorCorrectChangeUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change
  ) {
    if (urlContext == null) {
      return null;
    }

    return correctChangeUrl(urlContext, change, new CorrectChangeRoutes(
        ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(urlContext.correctionId(), urlContext.routingId(), null)),
        ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(urlContext.correctionId(), urlContext.routingId(), null)),
        ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForCorrectingChange(urlContext.correctionId(), urlContext.routingId(), change.changeId(), null))));
  }

  @Nullable
  private static String partialSurrenderCorrectUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change
  ) {
    if (urlContext == null) {
      return null;
    }

    var stagedTaskListUrl = stagedPartialSurrenderTaskListUrl(urlContext);
    var correctingChangeUrl = LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS.equals(change.changeType())
        ? stagedTaskListUrl
        : ReverseRouter.route(on(PartialSurrenderTaskListController.class).renderForCorrectingChange(
            urlContext.correctionId(), urlContext.routingId(), change.changeId(), null, null));

    return correctChangeUrl(
        urlContext, change, new CorrectChangeRoutes(stagedTaskListUrl, stagedTaskListUrl, correctingChangeUrl));
  }

  private static String stagedPartialSurrenderTaskListUrl(PositionChangeUrlContext urlContext) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(urlContext.correctionId(), urlContext.positionCorrectionId(), null, null));
  }

  @Nullable
  private static String correctChangeUrl(
      PositionChangeUrlContext urlContext,
      PositionChange change,
      CorrectChangeRoutes routes
  ) {
    if (urlContext.addedPosition()) {
      return routes.addedPosition();
    }
    if (LicencePositionChangeType.ADD_CHANGE.equals(change.changeType())) {
      return routes.executedPosition();
    }
    if (LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType())) {
      return null;
    }
    return routes.correctingChange();
  }

  private record CorrectChangeRoutes(
      String addedPosition,
      String executedPosition,
      String correctingChange
  ) {
  }

  /**
   * Only a live change left untouched by this correction can be removed, and only from the position that holds it.
   */
  @Nullable
  private static String removeChangeUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      Function<PositionChangeUrlContext, String> removeUrl
  ) {
    if (urlContext == null || urlContext.addedPosition() || change.changeType() != null) {
      return null;
    }

    return removeUrl.apply(urlContext);
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