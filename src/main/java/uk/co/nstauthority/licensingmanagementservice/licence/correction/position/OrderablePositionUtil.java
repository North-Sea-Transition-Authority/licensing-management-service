package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;

public class OrderablePositionUtil {

  private OrderablePositionUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static List<OrderablePosition> sameDatePositions(
      List<OrderablePosition> allOrderablePositions,
      UUID positionId
  ) {
    var movedPosition = allOrderablePositions.stream()
        .filter(position -> position.id().equals(positionId))
        .findFirst();
    if (movedPosition.isEmpty()) {
      return List.of();
    }

    var effectiveDate = movedPosition.get().effectiveDate();
    return allOrderablePositions.stream()
        .filter(position -> position.effectiveDate().equals(effectiveDate))
        .sorted(Comparator.comparingInt(OrderablePosition::effectiveDateOrder))
        .toList();
  }

  public static List<OrderablePosition> toOrderablePositions(CorrectionPositions correctionPositions) {
    var updatedPayloadsByPositionId = correctionPositions.updateCorrections()
        .stream()
        .collect(Collectors.toMap(
            correction -> correction.getTargetLicencePosition().getId(),
            correction -> (UpdateLicencePositionPayload) correction.getPayload()));

    var executedPositions = correctionPositions.executedPositions()
        .stream()
        .filter(position -> !correctionPositions.removedPositionIds().contains(position.getId()))
        .map(position -> toOrderablePosition(position, updatedPayloadsByPositionId.get(position.getId())));

    var addedPositions = correctionPositions.addCorrections()
        .stream()
        .map(correction -> (CreateLicencePositionPayload) correction.getPayload())
        .map(payload -> new OrderablePosition(
            UUID.fromString(payload.licencePositionId()),
            payload.effectiveDate(),
            payload.effectiveDateOrder(),
            payload.correctionReference(),
            true));

    return Stream.concat(executedPositions, addedPositions).toList();
  }

  private static OrderablePosition toOrderablePosition(LicencePosition position, UpdateLicencePositionPayload payload) {
    var effectiveDate = payload != null && payload.effectiveDate() != null
        ? payload.effectiveDate() : position.getPositionDate();
    var effectiveDateOrder = payload != null && payload.effectiveDateOrder() != null
        ? payload.effectiveDateOrder() : position.getPositionDateOrder();
    var reference = payload != null && payload.correctionReference() != null
        ? payload.correctionReference() : position.getLicenceTransaction().getRegulatorReference();
    return new OrderablePosition(position.getId(), effectiveDate, effectiveDateOrder, reference, false);
  }
}