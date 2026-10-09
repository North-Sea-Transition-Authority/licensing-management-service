package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.RemoveAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity.RemoveEquityChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee.LicencePositionLicenseeChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.RemovePartialSurrenderChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition.CorrectPositionChangeTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockRedefinitionOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
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
    return viewsForPosition(
        currentPositionId,
        chronologicalPositions,
        resolvedStates,
        new ChangeViewContext(organisationNames, featureNames, urlContext)
    )
        .stream()
        .map(ChangeViews::viewsByOperationType)
        .map(Map::values)
        .flatMap(Collection::stream)
        .toList();
  }

  public static Map<String, List<LicencePositionChangeView>> getChangeViewsByChangeId(
      UUID currentPositionId,
      List<ChronologicalPosition> chronologicalPositions,
      ResolvedStates resolvedStates,
      Map<Integer, String> organisationNames,
      Map<UUID, String> featureNames,
      @Nullable PositionChangeUrlContext urlContext
  ) {
    var viewsByChangeId = new LinkedHashMap<String, List<LicencePositionChangeView>>();

    viewsForPosition(
        currentPositionId,
        chronologicalPositions,
        resolvedStates,
        new ChangeViewContext(organisationNames, featureNames, urlContext))
        .forEach(changeViews ->
            viewsByChangeId.put(changeViews.changeId(), List.copyOf(changeViews.viewsByOperationType().values())));

    return viewsByChangeId;
  }

  private static List<ChangeViews> viewsByChange(
      UUID currentPositionId,
      List<PositionChange> currentPositionChanges,
      LicencePositionState stateBeforeCurrentPosition,
      @Nullable LocalDate currentPositionDate,
      ChangeViewContext context,
      boolean canReorder
  ) {
    var viewsByChange = new ArrayList<ChangeViews>();
    var stateBeforeChange = stateBeforeCurrentPosition;

    for (var change : currentPositionChanges) {
      var correctChangeOrderUrl = canReorder
          ? correctChangeOrderUrl(context.urlContext(), change, currentPositionId)
          : null;

      var correctPositionUrl = correctPositionUrl(context.urlContext(), change, currentPositionId);

      var changeViews = new LinkedHashMap<String, LicencePositionChangeView>();

      for (var operation : change.operations()) {
        var changeView = toView(
            operation,
            change,
            stateBeforeChange,
            currentPositionDate,
            context,
            correctChangeOrderUrl,
            correctPositionUrl
        );
        // An operation with no view of its own is not shown, rather than shown blank.
        if (changeView == null) {
          continue;
        }
        changeViews.merge(operation.type(), changeView, LicencePositionChangeView::merge);
      }
      stateBeforeChange = LicencePositionStateResolver.applyChange(stateBeforeChange, change);
      viewsByChange.add(new ChangeViews(change.changeId(), changeViews));
    }
    return viewsByChange;
  }

  private static List<ChangeViews> viewsForPosition(
      UUID currentPositionId,
      List<ChronologicalPosition> chronologicalPositions,
      ResolvedStates resolvedStates,
      ChangeViewContext context
  ) {
    var stateBeforeCurrentPosition = resolvedStates.previousState(currentPositionId);

    var currentPosition = chronologicalPositions.stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(currentPositionId))
        .toList();

    var currentPositionDate = getCurrentPositionDate(currentPosition);

    var currentPositionChanges = currentPosition.stream()
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .toList();

    var canReorder = currentPositionChanges.stream().filter(PositionChange::canBeReordered).count() > 1;

    return viewsByChange(
        currentPositionId,
        currentPositionChanges,
        stateBeforeCurrentPosition,
        currentPositionDate,
        context,
        canReorder
    );
  }

  public static Map<UUID, String> getOrderableChangeLabels(
      List<PositionChange> changes,
      Map<UUID, String> featureNames
  ) {
    var labels = new LinkedHashMap<UUID, String>();
    changes.stream()
        .filter(PositionChange::canBeReordered)
        .forEach(positionChange -> labels.put(
            UUID.fromString(positionChange.changeId()),
            orderableChangeLabel(positionChange, featureNames))
        );
    return labels;
  }

  private static String orderableChangeLabel(PositionChange change, Map<UUID, String> featureNames) {
    var operation = change.operations().getFirst();
    if (operation instanceof SubareaOperation subarea) {
      return "%s – %s".formatted(
          operation.displayName(),
          featureNames.getOrDefault(subarea.blockFeatureId(), NOT_AVAILABLE)
      );
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

  /**
   * The view an operation is shown as, or null where it has none.
   *
   * <p>The block, subarea creation and subarea end operations the PEARS migration produces are null
   * today: they are carried across so the spatial timeline is right, and nothing has been designed
   * for showing them on a position yet.
   */
  @Nullable
  private static LicencePositionChangeView toView(
      LicenceOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      ChangeViewContext context,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(
              administratorChange,
              change,
              previousState,
              context.organisationNames(),
              context.urlContext(),
              correctChangeOrderUrl,
              correctPositionUrl
          );
      case SetEquityOperation setEquityOperation ->
          buildSetEquityChangeView(
              setEquityOperation,
              change,
              context.organisationNames(),
              context.urlContext(),
              correctChangeOrderUrl,
              correctPositionUrl
          );
      case TransferEquityOperation transferEquityOperation ->
          buildTransferEquityChangeView(
              transferEquityOperation,
              change,
              previousState,
              context.organisationNames(),
              context.urlContext(),
              correctChangeOrderUrl,
              correctPositionUrl
          );
      case PartialSurrenderOperation partialSurrenderOperation ->
          buildPartialSurrenderChange(
              partialSurrenderOperation,
              change,
              currentPositionDate,
              context.featureNames(),
              context.urlContext(),
              correctChangeOrderUrl,
              correctPositionUrl
          );
      case SubareaOperation subareaOperation ->
          buildSubareaChange(
              subareaOperation,
              change,
              context.featureNames(),
              correctChangeOrderUrl,
              correctPositionUrl
          );
      case LicenseeOperation licenseeOperation ->
          buildLicenseeChange(
              licenseeOperation,
              change,
              context.organisationNames(),
              context.urlContext(),
              correctChangeOrderUrl,
              correctPositionUrl
          );
      case SubareaCreateOperation ignored -> null;
      case SubareaEndOperation ignored -> null;
      case BlockCreateOperation ignored -> null;
      case BlockRedefinitionOperation ignored -> null;
      case BlockEndOperation ignored -> null;
    };
  }

  private static LicenseeChangeView buildLicenseeChange(
      LicenseeOperation operation,
      PositionChange change,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
  ) {

    var licenseeToAddNames = licenseeIdToNames(operation.licenseesToAdd(), organisationNames);
    var licenseeToRemoveNames = licenseeIdToNames(operation.licenseesToRemove(), organisationNames);

    return new LicenseeChangeView(
        licenseeToRemoveNames,
        licenseeToAddNames,
        change.changeType(),
        new ChangeViewUrls(licenseeCorrectChangeUrl(urlContext, change), null, null, correctChangeOrderUrl, correctPositionUrl)
    );
  }

  private static List<String> licenseeIdToNames(List<Integer> licenseeIds, Map<Integer, String> organisationNames) {
    return licenseeIds
        .stream()
        .map(licenseeId -> organisationNames.getOrDefault(licenseeId, NOT_AVAILABLE))
        .toList();
  }

  private static SubareaChangeView buildSubareaChange(
      SubareaOperation operation,
      PositionChange change,
      Map<UUID, String> featureNames,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
  ) {
    return new SubareaChangeView(
        featureNames.getOrDefault(operation.blockFeatureId(), NOT_AVAILABLE),
        change.changeType(),
        new ChangeViewUrls(null, null, null, correctChangeOrderUrl, correctPositionUrl)
    );
  }

  private static PartialSurrenderChangeView buildPartialSurrenderChange(
      PartialSurrenderOperation operation,
      PositionChange change,
      @Nullable LocalDate currentPositionDate,
      Map<UUID, String> featureNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
  ) {
    var surrenderDate = operation.surrenderDate() != null ? operation.surrenderDate() : currentPositionDate;

    var blockRows = operation.surrenderedFeatureIds()
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
                        ctx.correction(), ctx.licencePosition(), changeEntity(change)))),
            undoChangeUrl(urlContext, change,
                ctx -> ReverseRouter.route(on(RemovePartialSurrenderChangeController.class)
                    .renderUndoPartialSurrender(ctx.correction(), change.changeId()))),
            correctChangeOrderUrl,
            correctPositionUrl
        )
    );
  }

  private static AdministratorChangeView buildAdministratorChange(
      AdministratorOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
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
                        ctx.correction(), ctx.licencePosition(), changeEntity(change)))),
            undoChangeUrl(urlContext, change,
                ctx -> ReverseRouter.route(on(RemoveAdministratorChangeController.class)
                    .renderUndoAdminChange(ctx.correction(), change.changeId()))),
            correctChangeOrderUrl,
            correctPositionUrl
        )
    );
  }

  private static SetEquityChangeView buildSetEquityChangeView(
      SetEquityOperation operation,
      PositionChange change,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
  ) {
    return new SetEquityChangeView(
        List.of(new SetEquityRow(organisationNames.getOrDefault(operation.transferTo(), NOT_AVAILABLE), operation.equity())),
        change.changeType(),
        new ChangeViewUrls(
            correctEquityChangeUrl(
                urlContext,
                change,
                ctx -> ReverseRouter.route(on(LicencePositionSetEquityController.class)
                    .renderSummaryForAddedPosition(ctx.correction(), ctx.positionCorrection())),
                ctx -> ReverseRouter.route(on(LicencePositionSetEquityController.class)
                    .renderSummaryForExecutedPosition(ctx.correction(), ctx.licencePosition()))),
            removeEquityChangeUrl(urlContext, change),
            undoEquityChangeUrl(urlContext, change),
            correctChangeOrderUrl,
            correctPositionUrl
        )
    );
  }

  private static TransferEquityChangeView buildTransferEquityChangeView(
      TransferEquityOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      @Nullable PositionChangeUrlContext urlContext,
      @Nullable String correctChangeOrderUrl,
      @Nullable String correctPositionUrl
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
                    .renderSummaryForAddedPosition(ctx.correction(), ctx.positionCorrection())),
                ctx -> ReverseRouter.route(on(LicencePositionTransferEquityController.class)
                    .renderSummaryForExecutedPosition(ctx.correction(), ctx.licencePosition()))
            ),
            removeEquityChangeUrl(urlContext, change),
            undoEquityChangeUrl(urlContext, change),
            correctChangeOrderUrl,
            correctPositionUrl
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
            .renderRemoveExecutedEquityChange(ctx.correction(), ctx.licencePosition(), changeEntity(change)))
    );
  }

  @Nullable
  private static String undoEquityChangeUrl(@Nullable PositionChangeUrlContext urlContext, PositionChange change) {
    return undoChangeUrl(
        urlContext,
        change,
        ctx -> ReverseRouter.route(on(RemoveEquityChangeController.class)
            .renderUndoEquityChange(ctx.correction(), change.changeId()))
    );
  }

  @Nullable
  private static String correctPositionUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      UUID currentPositionId
  ) {
    if (urlContext == null || !change.canBeReordered()) {
      return null;
    }
    return ReverseRouter.route(on(CorrectPositionChangeTypeController.class)
        .renderMoveChangeTypePosition(
            urlContext.correction().getId(), currentPositionId, UUID.fromString(change.changeId()), null));
  }

  @Nullable
  private static String correctChangeOrderUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      UUID currentPositionId
  ) {
    if (urlContext == null || !change.canBeReordered()) {
      return null;
    }
    return ReverseRouter.route(on(CorrectChangeOrderController.class)
        .renderCorrectChangeOrder(urlContext.correction(), currentPositionId, UUID.fromString(change.changeId())));
  }

  @Nullable
  private static String licenseeCorrectChangeUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change
  ) {
    if (urlContext == null) {
      return null;
    }

    return correctChangeUrl(urlContext, change, new CorrectChangeRoutes(
        ctx -> ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForAddedPosition(ctx.correction(), ctx.positionCorrection())),
        ctx -> ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForExecutedPosition(ctx.correction(), ctx.licencePosition())),
        ctx -> ReverseRouter.route(on(LicencePositionLicenseeChangeController.class)
            .renderForCorrectingChange(ctx.correction(), ctx.licencePosition(), changeEntity(change)))));
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
        ctx -> ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(ctx.correction(), ctx.positionCorrection())),
        ctx -> ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(ctx.correction(), ctx.licencePosition())),
        ctx -> ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForCorrectingChange(ctx.correction(), ctx.licencePosition(), changeEntity(change)))));
  }

  @Nullable
  private static String partialSurrenderCorrectUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change
  ) {
    if (urlContext == null) {
      return null;
    }

    Function<PositionChangeUrlContext, String> stagedTaskListUrl =
        LicencePositionChangeViewResolver::stagedPartialSurrenderTaskListUrl;
    Function<PositionChangeUrlContext, String> correctingChangeUrl =
        LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS.equals(change.changeType())
            ? stagedTaskListUrl
            : ctx -> ReverseRouter.route(on(PartialSurrenderTaskListController.class).renderForCorrectingChange(
                ctx.correction(), ctx.licencePosition(), changeEntity(change), null));

    return correctChangeUrl(
        urlContext, change, new CorrectChangeRoutes(stagedTaskListUrl, stagedTaskListUrl, correctingChangeUrl));
  }

  private static String stagedPartialSurrenderTaskListUrl(PositionChangeUrlContext urlContext) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(urlContext.correction(), urlContext.positionCorrection(), null));
  }

  @Nullable
  private static String correctChangeUrl(
      PositionChangeUrlContext urlContext,
      PositionChange change,
      CorrectChangeRoutes routes
  ) {
    if (urlContext.addedPosition()) {
      return routes.addedPosition().apply(urlContext);
    }
    if (LicencePositionChangeType.ADD_CHANGE.equals(change.changeType())) {
      return routes.executedPosition().apply(urlContext);
    }
    if (LicencePositionChangeType.REMOVE_CHANGE.equals(change.changeType())) {
      return null;
    }
    return routes.correctingChange().apply(urlContext);
  }

  private record CorrectChangeRoutes(
      Function<PositionChangeUrlContext, String> addedPosition,
      Function<PositionChangeUrlContext, String> executedPosition,
      Function<PositionChangeUrlContext, String> correctingChange
  ) {
  }

  private record ChangeViews(
      String changeId,
      Map<String, LicencePositionChangeView> viewsByOperationType
  ) {
  }

  private record ChangeViewContext(
      Map<Integer, String> organisationNames,
      Map<UUID, String> featureNames,
      @Nullable PositionChangeUrlContext urlContext
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

  /**
   * Only a change staged by this correction can be undone, whether it was added, corrected or removed.
   */
  @Nullable
  private static String undoChangeUrl(
      @Nullable PositionChangeUrlContext urlContext,
      PositionChange change,
      Function<PositionChangeUrlContext, String> undoUrl
  ) {
    if (urlContext == null || change.changeType() == null || change.movedAway()) {
      return null;
    }

    return undoUrl.apply(urlContext);
  }

  private static LicencePositionChange changeEntity(PositionChange change) {
    return new LicencePositionChange(UUID.fromString(change.changeId()));
  }
}
