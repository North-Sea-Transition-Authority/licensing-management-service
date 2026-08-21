package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import jakarta.annotation.Nullable;
import java.util.UUID;

/**
 * Identifies the position a change is being viewed on within a correction, so change action urls can be built.
 *
 * @param correctionId the licence correction the position is being viewed within
 * @param addedPosition true when the position is being added by this correction, as opposed to an executed position
 * @param routingId the licence position id for an executed position, or the licence position correction id for one
 *                  added by this correction
 * @param positionCorrectionId the correction staging changes against that position; null for an executed position
 *                             this correction has not touched yet
 */
public record PositionChangeUrlContext(
    UUID correctionId,
    boolean addedPosition,
    UUID routingId,
    @Nullable UUID positionCorrectionId
) {

  public static PositionChangeUrlContext forExecutedPosition(
      UUID correctionId,
      UUID licencePositionId,
      @Nullable UUID positionCorrectionId
  ) {
    return new PositionChangeUrlContext(correctionId, false, licencePositionId, positionCorrectionId);
  }

  public static PositionChangeUrlContext forAddedPosition(UUID correctionId, UUID licencePositionCorrectionId) {
    return new PositionChangeUrlContext(
        correctionId, true, licencePositionCorrectionId, licencePositionCorrectionId);
  }
}
