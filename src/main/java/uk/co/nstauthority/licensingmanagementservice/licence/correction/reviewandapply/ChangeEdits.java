package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

record ChangeEdits(
    Set<String> correctedChangeIds,
    Map<String, Map<String, LicencePositionChangeView>> executedViewsByChangeId,
    Map<String, MovedChange> movedChanges
) {

  static ChangeEdits from(
      List<LicencePositionCorrection> positionCorrections,
      ReviewPositionContext correctedContext,
      ReviewPositionContext executedContext
  ) {
    var correctionChanges = positionCorrections.stream()
        .map(LicencePositionCorrection::getPayload)
        .filter(Objects::nonNull)
        .flatMap(payload -> payload.changes().stream())
        .toList();

    var correctedChanges = changesByChangeId(correctedContext.positions());
    var executedChanges = changesByChangeId(executedContext.positions());
    var movedChanges = movedChanges(positionCorrections, correctedContext, executedContext, executedChanges);

    var correctedChangeIds = changeIdsOfType(correctionChanges, UpdateChangeOperations.class).stream()
        .filter(changeId -> !movedChanges.containsKey(changeId)
            || operationsDiffer(correctedChanges.get(changeId), executedChanges.get(changeId)))
        .collect(Collectors.toSet());

    return new ChangeEdits(
        correctedChangeIds,
        executedViewsByChangeId(executedContext),
        movedChanges
    );
  }

  Set<UUID> movedFromPositionIds() {
    return movedChanges.values().stream()
        .map(MovedChange::fromPositionId)
        .collect(Collectors.toSet());
  }

  ReviewChangeView reviewChange(LicencePositionChangeView change, String changeId, @Nullable Integer changeOrder) {
    var movedChange = movedChanges.get(changeId);
    return new ReviewChangeView(
        change,
        correctedChangeIds.contains(changeId) ? executedView(changeId, change.type()) : null,
        changeOrder,
        movedChange != null ? movedChange.movedFrom() : null,
        null
    );
  }

  List<ReviewChangeView> movedAwayChanges(UUID positionId) {
    return movedChanges.values().stream()
        .filter(movedChange -> movedChange.fromPositionId().equals(positionId))
        .flatMap(movedChange -> executedViewsByChangeId.getOrDefault(movedChange.changeId(), Map.of())
            .values()
            .stream()
            .map(executedView -> new ReviewChangeView(
                executedView,
                null,
                movedChange.liveChangeOrder(),
                null,
                movedChange.movedTo()
            )))
        .toList();
  }

  @Nullable
  private LicencePositionChangeView executedView(String changeId, String operationType) {
    return executedViewsByChangeId.getOrDefault(changeId, Map.of()).get(operationType);
  }

  private static Map<String, MovedChange> movedChanges(
      List<LicencePositionCorrection> positionCorrections,
      ReviewPositionContext correctedContext,
      ReviewPositionContext executedContext,
      Map<String, PositionChange> executedChanges
  ) {
    var livePositionsByChangeId = positionsByChangeId(executedContext.positions());
    var correctedPositionsById = correctedContext.positions().stream()
        .collect(Collectors.toMap(ChronologicalPosition::id, Function.identity()));

    var movedChanges = new HashMap<String, MovedChange>();

    LicencePositionCorrectionService.getUpdatedChangePositionIds(positionCorrections)
        .forEach((changeId, targetPositionId) -> {
          var livePosition = livePositionsByChangeId.get(changeId);
          var targetPosition = correctedPositionsById.get(targetPositionId);
          if (livePosition == null || targetPosition == null || livePosition.id().equals(targetPositionId)) {
            return;
          }
          movedChanges.put(changeId, new MovedChange(
              changeId,
              livePosition.id(),
              positionLabel(correctedPositionsById.getOrDefault(livePosition.id(), livePosition)),
              positionLabel(targetPosition),
              executedChanges.get(changeId).changeOrder()
          ));
        });

    return movedChanges;
  }

  private static String positionLabel(ChronologicalPosition position) {
    return "%s - %s".formatted(position.positionName(), position.reference());
  }

  private static boolean operationsDiffer(
      @Nullable PositionChange correctedChange,
      @Nullable PositionChange executedChange
  ) {
    if (correctedChange == null || executedChange == null) {
      return true;
    }
    return !correctedChange.operations().equals(executedChange.operations());
  }

  private static Map<String, ChronologicalPosition> positionsByChangeId(List<ChronologicalPosition> positions) {
    var positionsByChangeId = new HashMap<String, ChronologicalPosition>();
    positions.forEach(position -> position.changes()
        .forEach(change -> positionsByChangeId.put(change.changeId(), position)));
    return positionsByChangeId;
  }

  private static Map<String, PositionChange> changesByChangeId(List<ChronologicalPosition> positions) {
    var changesByChangeId = new HashMap<String, PositionChange>();
    positions.forEach(position -> position.changes()
        .forEach(change -> changesByChangeId.put(change.changeId(), change)));
    return changesByChangeId;
  }

  private static Set<String> changeIdsOfType(
      List<LicencePositionChangeType> correctionChanges,
      Class<? extends LicencePositionChangeType> changeType
  ) {
    return correctionChanges.stream()
        .filter(changeType::isInstance)
        .map(LicencePositionChangeType::changeId)
        .collect(Collectors.toSet());
  }

  private static Map<String, Map<String, LicencePositionChangeView>> executedViewsByChangeId(
      ReviewPositionContext executedContext
  ) {
    var viewsByChangeId = new HashMap<String, Map<String, LicencePositionChangeView>>();

    executedContext.positions().forEach(position ->
        LicencePositionChangeViewResolver.getChangeViewsByChangeId(
            position.id(),
            executedContext.positions(),
            executedContext.resolvedStates(),
            executedContext.organisationNames(),
            executedContext.featureNames(),
            null
        ).forEach((changeId, views) -> viewsByChangeId.put(changeId, byOperationType(views))));

    return viewsByChangeId;
  }

  private static Map<String, LicencePositionChangeView> byOperationType(List<LicencePositionChangeView> views) {
    return views.stream()
        .collect(Collectors.toMap(LicencePositionChangeView::type, Function.identity()));
  }
}
