package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionOrderingUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Service
public class CorrectChangeOrderService {

  private final LicencePositionViewService licencePositionViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;

  public CorrectChangeOrderService(
      LicencePositionViewService licencePositionViewService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService
  ) {
    this.licencePositionViewService = licencePositionViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
  }

  public List<OrderableChange> getOrderableChanges(LicenceCorrection licenceCorrection, UUID positionId) {
    return licencePositionViewService.getOrderableChangeLabels(licenceCorrection, positionId)
        .entrySet().stream()
        .map(entry -> new OrderableChange(entry.getKey(), entry.getValue()))
        .toList();
  }

  @Transactional
  public void correctChangeOrder(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      UUID movedChangeId,
      UUID targetChangeId,
      PositionMoveDirection direction
  ) {
    var changes = getOrderableChanges(licenceCorrection, licencePositionId);

    var moved = changes.stream()
        .filter(orderableChange -> orderableChange.id().equals(movedChangeId))
        .findFirst().orElseThrow(() -> new IllegalArgumentException(
            "Cannot move change %s as it is not on licence position %s".formatted(movedChangeId, licencePositionId)));

    var target = changes.stream()
        .filter(orderableChange -> orderableChange.id().equals(targetChangeId))
        .findFirst().orElseThrow(() -> new IllegalArgumentException(
            "Cannot move change %s relative to change %s as they are not on the same licence position %s"
                .formatted(movedChangeId, targetChangeId, licencePositionId)
        ));

    var newChangeOrders = newChangeOrders(
        PositionOrderingUtil.moveRelativeTo(changes, moved, target, direction)
    );

    licencePositionCorrectionService.findFirstAddedPositionCorrection(licenceCorrection, licencePositionId).ifPresentOrElse(
        addedPositionCorrection -> reorderAddedPositionChanges(addedPositionCorrection, newChangeOrders),
        () -> reorderExecutedPositionChanges(licenceCorrection, licencePositionId, newChangeOrders));
  }

  @Transactional
  public void moveChangeToPosition(
      LicenceCorrection licenceCorrection,
      UUID sourcePositionId,
      UUID changeId,
      UUID targetPositionId
  ) {
    if (sourcePositionId.equals(targetPositionId)) {
      throw new IllegalArgumentException(
          "Cannot move change %s to licence position %s as it is already on it".formatted(changeId, targetPositionId)
      );
    }

    if (getOrderableChanges(licenceCorrection, sourcePositionId).stream()
        .noneMatch(orderableChange -> orderableChange.id().equals(changeId))) {
      throw new IllegalArgumentException(
          "Cannot move change %s as it is not on licence position %s".formatted(changeId, sourcePositionId)
      );
    }

    var sourcePositionCorrection =
        licencePositionCorrectionService.getOrBuildPositionCorrection(licenceCorrection, sourcePositionId);
    var stagedChange = licencePositionCorrectionService
        .findStagedChange(sourcePositionCorrection, changeId.toString()).orElse(null);

    licencePositionCorrectionService.dropStagedChangeAndOrder(sourcePositionCorrection, changeId.toString());

    if (stagedChange instanceof AddChange addChange) {
      licencePositionCorrectionService.stageChangesOnPosition(
          licenceCorrection,
          targetPositionId,
          List.of(
              new AddChange(
                  addChange.changeId(),
                  nextChangeOrderOn(licenceCorrection, targetPositionId, changeId),
                  addChange.operations()
              )
          )
      );
    } else {
      moveLiveChange(licenceCorrection, changeId, stagedChange, targetPositionId);
    }

    partialSurrenderCorrectionService.adjustPartialSurrenderBlocksFrom(
        licenceCorrection,
        sourcePositionId,
        targetPositionId
    );
  }

  private void moveLiveChange(
      LicenceCorrection licenceCorrection,
      UUID changeId,
      @Nullable LicencePositionChangeType stagedChange,
      UUID targetPositionId
  ) {
    var liveChange = licencePositionChangeService.getByIdOrThrow(changeId);
    var movedChange = stagedChange instanceof UpdateChangeOperations stagedEdits
        ? stagedEdits
        : UpdateChangeOperations.buildUpdateChange(
            changeId.toString(),
            LicencePositionChange.operationsOf(liveChange)
        );

    var isTargetPositionLive = liveChange.getLicencePosition().getId().equals(targetPositionId);
    var changeOrder = nextChangeOrderOn(licenceCorrection, targetPositionId, changeId);
    var changesToStage = new ArrayList<LicencePositionChangeType>();

    if (!isTargetPositionLive
        || !LicencePositionChangeType.operationsOf(movedChange).equals(LicencePositionChange.operationsOf(liveChange))) {
      changesToStage.add(movedChange);
    }

    if (!isTargetPositionLive || changeOrder != liveChange.getChangeOrder()) {
      changesToStage.add(LicencePositionChangeType.updateChangeOrder()
          .withChangeId(movedChange.changeId())
          .withChangeOrder(changeOrder)
          .build());
    }

    if (!changesToStage.isEmpty()) {
      licencePositionCorrectionService.stageChangesOnPosition(licenceCorrection, targetPositionId, changesToStage);
    }
  }

  private int nextChangeOrderOn(LicenceCorrection licenceCorrection, UUID licencePositionId, UUID movedChangeId) {
    var positionCorrection =
        licencePositionCorrectionService.getOrBuildPositionCorrection(licenceCorrection, licencePositionId);

    var changes = positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION
        ? licencePositionCorrectionService.getChangesForAddedPosition(positionCorrection)
        : licencePositionCorrectionService.getChangesForExecutedPosition(
            licenceCorrection,
            positionCorrection.getTargetLicencePosition(),
            positionCorrection
        );

    var changeOrders = new HashMap<>(liveChangeOrdersByChangeId(licencePositionId));
    changes.forEach(change -> changeOrders.put(change.changeId(), change.changeOrder()));
    changeOrders.remove(movedChangeId.toString());

    return changeOrders.values().stream()
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;
  }

  private static Map<String, Integer> newChangeOrders(List<OrderableChange> reorderedChanges) {
    var newChangeOrders = new LinkedHashMap<String, Integer>();
    for (var change : reorderedChanges) {
      newChangeOrders.put(change.id().toString(), newChangeOrders.size() + 1);
    }
    return newChangeOrders;
  }

  private void reorderExecutedPositionChanges(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId,
      Map<String, Integer> newChangeOrders
  ) {
    var position = licencePositionService.getPositionForLicence(licenceCorrection.getLicence(), licencePositionId);
    var liveChangeOrders = liveChangeOrdersByChangeId(licencePositionId);
    var positionCorrection =
        licencePositionCorrectionService.getOrBuildUpdatePositionCorrection(licenceCorrection, position);

    var payload = positionCorrection.getPayload();
    var updatedChanges = new ArrayList<>(payload.changes());
    updatedChanges.removeIf(UpdateChangeOrder.class::isInstance);

    updatedChanges.replaceAll(change -> withNewOrder(change, newChangeOrders));

    var addedChangeIds = updatedChanges.stream()
        .filter(AddChange.class::isInstance)
        .map(LicencePositionChangeType::changeId)
        .collect(Collectors.toSet());

    newChangeOrders.forEach((changeId, newChangeOrder) -> {
      if (!addedChangeIds.contains(changeId) && !Objects.equals(newChangeOrder, liveChangeOrders.get(changeId))) {
        updatedChanges.add(LicencePositionChangeType.updateChangeOrder()
            .withChangeId(changeId)
            .withChangeOrder(newChangeOrder)
            .build());
      }
    });

    licencePositionCorrectionService.saveOrDiscard(positionCorrection, updatedChanges);
  }

  private void reorderAddedPositionChanges(
      LicencePositionCorrection addedPositionCorrection,
      Map<String, Integer> newChangeOrders
  ) {
    var payload = addedPositionCorrection.getPayload();
    var updatedChanges = new ArrayList<>(payload.changes());
    updatedChanges.replaceAll(change -> withNewOrder(change, newChangeOrders));

    addedPositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, updatedChanges));
    licencePositionCorrectionService.save(addedPositionCorrection);
  }

  private Map<String, Integer> liveChangeOrdersByChangeId(UUID licencePositionId) {
    return licencePositionChangeService.findByLicencePositionId(licencePositionId)
        .stream()
        .collect(Collectors.toMap(
            change -> change.getId().toString(),
            LicencePositionChange::getChangeOrder));
  }

  private static LicencePositionChangeType withNewOrder(
      LicencePositionChangeType change,
      Map<String, Integer> newChangeOrders
  ) {
    if (!newChangeOrders.containsKey(change.changeId())) {
      return change;
    }
    var newChangeOrder = newChangeOrders.get(change.changeId());

    return switch (change) {
      case AddChange addChange -> new AddChange(addChange.changeId(), newChangeOrder, addChange.operations());
      case UpdateChangeOrder updateChangeOrder -> new UpdateChangeOrder(updateChangeOrder.changeId(), newChangeOrder);
      default -> change;
    };
  }
}