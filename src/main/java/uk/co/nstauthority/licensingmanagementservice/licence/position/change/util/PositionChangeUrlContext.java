package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

/**
 * Identifies the position a change is being viewed on within a correction, so change action urls can be built.
 *
 * @param correction the licence correction the position is being viewed within
 * @param licencePosition the executed position; null for a position added by this correction
 * @param positionCorrection the correction staging changes against that position; null for an executed position
 *                           this correction has not touched yet
 */
public record PositionChangeUrlContext(
    LicenceCorrection correction,
    @Nullable LicencePosition licencePosition,
    @Nullable LicencePositionCorrection positionCorrection
) {

  public static PositionChangeUrlContext forExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @Nullable LicencePositionCorrection positionCorrection
  ) {
    return new PositionChangeUrlContext(correction, licencePosition, positionCorrection);
  }

  public static PositionChangeUrlContext forAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return new PositionChangeUrlContext(correction, null, licencePositionCorrection);
  }

  public boolean addedPosition() {
    return licencePosition == null;
  }
}
