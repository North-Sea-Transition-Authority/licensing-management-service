package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionOrderingUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.AddChange;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.UpdateChangeOrder;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

@Service
public class CorrectChangeOrderService {

  private final LicencePositionViewService licencePositionViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;

  public CorrectChangeOrderService(
      LicencePositionViewService licencePositionViewService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService
  ) {
    this.licencePositionViewService = licencePositionViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
  }

  public List<OrderableChange> getOrderableChanges(LicenceCorrection licenceCorrection, UUID licencePositionId) {
    return orderableChangeTypeGroups(licenceCorrection, licencePositionId).stream()
        .map(typeGroup -> new OrderableChange(typeGroup.representativeChangeId(), typeGroup.typeName()))
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
    var typeGroups = orderableChangeTypeGroups(licenceCorrection, licencePositionId);

    var movedGroup = findChangeWithId(typeGroups, movedChangeId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Cannot move change %s as it is not on licence position %s".formatted(movedChangeId, licencePositionId)));

    var targetGroup = findChangeWithId(typeGroups, targetChangeId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Cannot move change %s relative to change %s as they are not on the same licence position %s"
                .formatted(movedChangeId, targetChangeId, licencePositionId)));

    if (movedGroup.equals(targetGroup)) {
      throw new IllegalArgumentException(
          "Cannot move change %s relative to change %s as both are part of the same change type: %s"
              .formatted(movedChangeId, targetChangeId, movedGroup.typeName()));
    }

    var newChangeOrders = newChangeOrders(
        PositionOrderingUtil.moveRelativeTo(typeGroups, movedGroup, targetGroup, direction));

    licencePositionCorrectionService.findFirstAddedPositionCorrection(licenceCorrection, licencePositionId).ifPresentOrElse(
        addedPositionCorrection -> reorderAddedPositionChanges(addedPositionCorrection, newChangeOrders),
        () -> reorderExecutedPositionChanges(licenceCorrection, licencePositionId, newChangeOrders));
  }

  private static Map<String, Integer> newChangeOrders(List<ChangeTypeGroup> reorderedTypeGroups) {
    var newChangeOrders = new LinkedHashMap<String, Integer>();
    for (var changeId : reorderedTypeGroups.stream().flatMap(group -> group.changeIds().stream()).toList()) {
      newChangeOrders.put(changeId.toString(), newChangeOrders.size() + 1);
    }
    return newChangeOrders;
  }

  private List<ChangeTypeGroup> orderableChangeTypeGroups(
      LicenceCorrection licenceCorrection,
      UUID licencePositionId
  ) {
    var changesByOperationType = orderableChanges(licenceCorrection, licencePositionId)
        .stream()
        .collect(Collectors.groupingBy(
            change -> change.operations().getFirst().type(),
            LinkedHashMap::new,
            Collectors.toList()
        ));

    return changesByOperationType.values().stream()
        .map(ChangeTypeGroup::fromChangesOfOneType)
        .toList();
  }

  private List<PositionChange> orderableChanges(LicenceCorrection licenceCorrection, UUID licencePositionId) {
    return licencePositionViewService.getCorrectedChronologicalPositions(licenceCorrection, licencePositionId)
        .stream()
        .filter(chronologicalPosition -> chronologicalPosition.id().equals(licencePositionId))
        .flatMap(chronologicalPosition -> chronologicalPosition.changes().stream())
        .filter(PositionChange::isOrderable)
        .toList();
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

    updatedChanges.replaceAll(change -> withNewOrderIfAddChange(change, newChangeOrders));

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

    saveOrDiscardPositionCorrection(positionCorrection, payload, updatedChanges);
  }

  private void reorderAddedPositionChanges(
      LicencePositionCorrection addedPositionCorrection,
      Map<String, Integer> newChangeOrders
  ) {
    var payload = addedPositionCorrection.getPayload();
    var updatedChanges = new ArrayList<>(payload.changes());
    updatedChanges.replaceAll(change -> withNewOrderIfAddChange(change, newChangeOrders));

    addedPositionCorrection.setPayload(LicencePositionPayload.withChanges(payload, updatedChanges));
    licencePositionCorrectionService.save(addedPositionCorrection);
  }

  private void saveOrDiscardPositionCorrection(
      LicencePositionCorrection positionCorrection,
      LicencePositionPayload payload,
      List<LicencePositionChangeType> updatedChanges
  ) {
    positionCorrection.setPayload(LicencePositionPayload.withChanges(payload, updatedChanges));

    var nothingLeftToCorrect = updatedChanges.isEmpty()
        && LicencePositionChangeUtil.positionDateAndOrderUnchanged(positionCorrection);

    if (!nothingLeftToCorrect) {
      licencePositionCorrectionService.save(positionCorrection);
    } else if (positionCorrection.getId() != null) {
      licencePositionCorrectionService.delete(positionCorrection);
    }
  }

  private Map<String, Integer> liveChangeOrdersByChangeId(UUID licencePositionId) {
    return licencePositionChangeService.findByLicencePositionId(licencePositionId)
        .stream()
        .collect(Collectors.toMap(
            change -> change.getId().toString(),
            LicencePositionChange::getChangeOrder));
  }

  private static Optional<ChangeTypeGroup> findChangeWithId(List<ChangeTypeGroup> typeGroups, UUID changeId) {
    return typeGroups.stream()
        .filter(typeGroup -> typeGroup.contains(changeId))
        .findFirst();
  }

  private static LicencePositionChangeType withNewOrderIfAddChange(
      LicencePositionChangeType change,
      Map<String, Integer> newChangeOrders
  ) {
    if (change instanceof AddChange addChange && newChangeOrders.containsKey(addChange.changeId())) {
      return new AddChange(addChange.changeId(), newChangeOrders.get(addChange.changeId()), addChange.operations());
    }
    return change;
  }

  private record ChangeTypeGroup(String typeName, List<UUID> changeIds) {

    static ChangeTypeGroup fromChangesOfOneType(List<PositionChange> changes) {
      return new ChangeTypeGroup(
          changes.getFirst().operations().getFirst().displayName(),
          changes.stream().map(change -> UUID.fromString(change.changeId())).toList()
      );
    }

    UUID representativeChangeId() {
      return changeIds.getFirst();
    }

    boolean contains(UUID changeId) {
      return changeIds.contains(changeId);
    }
  }
}