package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

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
        new LicencePositionChangeViewContext(organisationNames, featureNames),
        urlContext
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
        new LicencePositionChangeViewContext(organisationNames, featureNames),
        urlContext
    )
        .forEach(changeViews ->
            viewsByChangeId.put(changeViews.changeId(), List.copyOf(changeViews.viewsByOperationType().values())));

    return viewsByChangeId;
  }

  private static List<ChangeViews> viewsByChange(
      UUID currentPositionId,
      List<PositionChange> currentPositionChanges,
      LicencePositionState stateBeforeCurrentPosition,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext viewContext,
      @Nullable PositionChangeUrlContext urlContext,
      boolean canReorder
  ) {
    var viewsByChange = new ArrayList<ChangeViews>();
    var stateBeforeChange = stateBeforeCurrentPosition;

    for (var change : currentPositionChanges) {
      var changeViews = new LinkedHashMap<String, LicencePositionChangeView>();

      for (var operation : change.visibleLicenceOperations()) {
        var urls = ChangeViewUrlsFactory.build(urlContext, change, currentPositionId, canReorder, operation);
        var changeView = operation.getChangeView(change, stateBeforeChange, currentPositionDate, viewContext, urls);
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
      LicencePositionChangeViewContext viewContext,
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

    var canReorder = currentPositionChanges.stream().filter(PositionChange::canBeReordered).count() > 1;

    return viewsByChange(
        currentPositionId,
        currentPositionChanges,
        stateBeforeCurrentPosition,
        currentPositionDate,
        viewContext,
        urlContext,
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

  private record ChangeViews(
      String changeId,
      Map<String, LicencePositionChangeView> viewsByOperationType
  ) {
  }
}
