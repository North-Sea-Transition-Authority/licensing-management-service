package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import java.util.UUID;

public record PositionChangeUrlContext(
    UUID correctionId,
    boolean addedPosition,
    UUID routingId
) {

  public static PositionChangeUrlContext forExecutedPosition(UUID correctionId, UUID licencePositionId) {
    return new PositionChangeUrlContext(correctionId, false, licencePositionId);
  }

  public static PositionChangeUrlContext forAddedPosition(UUID correctionId, UUID licencePositionCorrectionId) {
    return new PositionChangeUrlContext(correctionId, true, licencePositionCorrectionId);
  }
}
