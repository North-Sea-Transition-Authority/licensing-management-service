package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeViewResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

record ChangeEdits(
    Set<String> correctedChangeIds,
    Map<String, Map<String, LicencePositionChangeView>> executedViewsByChangeId
) {

  static ChangeEdits from(
      List<LicencePositionCorrection> positionCorrections,
      ReviewPositionContext executedContext
  ) {
    var correctionChanges = positionCorrections.stream()
        .map(LicencePositionCorrection::getPayload)
        .filter(Objects::nonNull)
        .flatMap(payload -> payload.changes().stream())
        .toList();

    return new ChangeEdits(
        changeIdsOfType(correctionChanges, UpdateChangeOperations.class),
        executedViewsByChangeId(executedContext)
    );
  }

  ReviewChangeView reviewChange(LicencePositionChangeView change, String changeId, @Nullable Integer changeOrder) {
    return new ReviewChangeView(
        change,
        correctedChangeIds.contains(changeId) ? executedView(changeId, change.type()) : null,
        changeOrder
    );
  }

  @Nullable
  private LicencePositionChangeView executedView(String changeId, String operationType) {
    return executedViewsByChangeId.getOrDefault(changeId, Map.of()).get(operationType);
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
