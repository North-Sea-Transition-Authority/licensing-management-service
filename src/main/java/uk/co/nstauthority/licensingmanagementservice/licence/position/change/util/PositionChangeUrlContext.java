package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;

/**
 * Identifies the position a change is being viewed on, so change action urls can be built.
 *
 * @param correction the licence correction the position is being viewed within; null on the licence timeline, where
 *                   only the view url is built
 * @param licencePosition the executed position; null for a position added by this correction
 * @param positionStagedPayload the staged payload holding changes against that position; null for an executed
 *                              position with no staged payload yet, and on the licence timeline
 */
public record PositionChangeUrlContext(
    @Nullable LicenceCorrection correction,
    @Nullable LicencePosition licencePosition,
    @Nullable LicencePositionStagedPayload positionStagedPayload
) {

  public static PositionChangeUrlContext forExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @Nullable LicencePositionStagedPayload positionStagedPayload
  ) {
    return new PositionChangeUrlContext(correction, licencePosition, positionStagedPayload);
  }

  public static PositionChangeUrlContext forAddedPosition(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload
  ) {
    return new PositionChangeUrlContext(correction, null, licencePositionStagedPayload);
  }

  public static PositionChangeUrlContext forTimelinePosition(LicencePosition licencePosition) {
    return new PositionChangeUrlContext(null, licencePosition, null);
  }

  /**
   * The position as the correction sees it. Only available within a correction.
   */
  public CorrectionPosition correctionPosition() {
    if (correction == null) {
      throw new IllegalStateException("A position on the licence timeline is not being viewed within a correction");
    }
    return addedPosition()
        ? new CorrectionPosition.Staged(positionStagedPayload)
        : new CorrectionPosition.Executed(correction, licencePosition);
  }

  public boolean addedPosition() {
    return licencePosition == null;
  }

  public boolean inCorrection() {
    return correction != null;
  }
}
