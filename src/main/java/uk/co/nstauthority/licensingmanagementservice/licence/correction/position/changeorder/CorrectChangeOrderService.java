package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import java.util.ArrayList;
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

  private static LicencePositionChangeType withNewOrderIfAddChange(
      LicencePositionChangeType change,
      Map<String, Integer> newChangeOrders
  ) {
    if (change instanceof AddChange addChange && newChangeOrders.containsKey(addChange.changeId())) {
      return new AddChange(addChange.changeId(), newChangeOrders.get(addChange.changeId()), addChange.operations());
    }
    return change;
  }
}