package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

/**
 * What a change form acts on: a new change on a position, or a correction to a live change.
 */
public sealed interface ChangeFormTarget {

  LicenceCorrection correction();

  /**
   * The position the change is on, whether executed or added by the correction.
   */
  UUID positionId();

  /**
   * A new change on a position on the correction's timeline, whether executed or added by the correction.
   */
  record NewChange(CorrectionPosition correctionPosition) implements ChangeFormTarget {

    @Override
    public LicenceCorrection correction() {
      return correctionPosition.correction();
    }

    @Override
    public UUID positionId() {
      return correctionPosition.positionId();
    }
  }

  record LiveChange(
      LicenceCorrection correction,
      LicencePosition position,
      LicencePositionChange change
  ) implements ChangeFormTarget {

    @Override
    public UUID positionId() {
      return position.getId();
    }
  }
}
