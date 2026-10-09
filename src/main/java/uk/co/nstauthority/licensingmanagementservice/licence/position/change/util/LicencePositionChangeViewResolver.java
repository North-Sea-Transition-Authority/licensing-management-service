package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;
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
      var changeViews = new LinkedHashMap<String, LicencePositionChangeView>();

      for (var operation : change.visibleLicenceOperations()) {
        var urls = ChangeViewUrlsFactory.build(context.urlContext(), change, currentPositionId, canReorder, operation);
        var changeView = toView(operation, change, stateBeforeChange, currentPositionDate, context, urls);
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
   * The view an operation is shown as.
   *
   * @param urls the actions the view offers
   */
  private static LicencePositionChangeView toView(
      VisibleLicenceOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      ChangeViewContext context,
      ChangeViewUrls urls
  ) {
    return switch (operation) {
      case AdministratorOperation administratorChange ->
          buildAdministratorChange(administratorChange, change, previousState, context.organisationNames(), urls);
      case SetEquityOperation setEquityOperation ->
          buildSetEquityChangeView(setEquityOperation, change, context.organisationNames(), urls);
      case TransferEquityOperation transferEquityOperation ->
          buildTransferEquityChangeView(
              transferEquityOperation,
              change,
              previousState,
              context.organisationNames(),
              urls
          );
      case PartialSurrenderOperation partialSurrenderOperation ->
          buildPartialSurrenderChange(
              partialSurrenderOperation,
              change,
              currentPositionDate,
              context.featureNames(),
              urls
          );
      case SubareaOperation subareaOperation ->
          buildSubareaChange(subareaOperation, change, context.featureNames(), urls);
      case LicenseeOperation licenseeOperation ->
          buildLicenseeChange(licenseeOperation, change, context.organisationNames(), urls);
    };
  }

  private static LicenseeChangeView buildLicenseeChange(
      LicenseeOperation operation,
      PositionChange change,
      Map<Integer, String> organisationNames,
      ChangeViewUrls urls
  ) {

    var licenseeToAddNames = licenseeIdToNames(operation.licenseesToAdd(), organisationNames);
    var licenseeToRemoveNames = licenseeIdToNames(operation.licenseesToRemove(), organisationNames);

    return new LicenseeChangeView(
        licenseeToRemoveNames,
        licenseeToAddNames,
        change.changeType(),
        urls
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
      ChangeViewUrls urls
  ) {
    return new SubareaChangeView(
        featureNames.getOrDefault(operation.blockFeatureId(), NOT_AVAILABLE),
        change.changeType(),
        urls
    );
  }

  private static PartialSurrenderChangeView buildPartialSurrenderChange(
      PartialSurrenderOperation operation,
      PositionChange change,
      @Nullable LocalDate currentPositionDate,
      Map<UUID, String> featureNames,
      ChangeViewUrls urls
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
        urls
    );
  }

  private static AdministratorChangeView buildAdministratorChange(
      AdministratorOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      ChangeViewUrls urls
  ) {
    var joiningId = operation.operatorId();

    var withdrawingId = previousState.administratorId();
    var withdrawingName = (withdrawingId == null) ? null : organisationNames.getOrDefault(withdrawingId, NOT_AVAILABLE);

    return new AdministratorChangeView(
        withdrawingName,
        organisationNames.getOrDefault(joiningId, NOT_AVAILABLE),
        change.changeId(),
        change.changeType(),
        urls
    );
  }

  private static SetEquityChangeView buildSetEquityChangeView(
      SetEquityOperation operation,
      PositionChange change,
      Map<Integer, String> organisationNames,
      ChangeViewUrls urls
  ) {
    return new SetEquityChangeView(
        List.of(new SetEquityRow(organisationNames.getOrDefault(operation.transferTo(), NOT_AVAILABLE), operation.equity())),
        change.changeType(),
        urls
    );
  }

  private static TransferEquityChangeView buildTransferEquityChangeView(
      TransferEquityOperation operation,
      PositionChange change,
      LicencePositionState previousState,
      Map<Integer, String> organisationNames,
      ChangeViewUrls urls
  ) {
    var transferFrom = operation.transferFrom();
    var transferTo = operation.transferTo();

    var startingEquityByOrganisationId = previousState.equityByOrganisationId();
    var resultingEquityByOrganisationId =
        operation.applyState(previousState).equityByOrganisationId();

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
        urls
    );
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
}
