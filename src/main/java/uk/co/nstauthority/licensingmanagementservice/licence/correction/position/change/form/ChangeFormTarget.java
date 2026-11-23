package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

/**
 * What a change form acts on: a new change on an executed position, a new change on a position the correction has
 * added, or a correction to a live change.
 */
public sealed interface ChangeFormTarget {

  LicenceCorrection correction();

  /**
   * The position the change is on, whether executed or added by the correction.
   */
  UUID positionId();

  record ExecutedPosition(LicenceCorrection correction, LicencePosition position) implements ChangeFormTarget {

    @Override
    public UUID positionId() {
      return position.getId();
    }
  }

  record AddedPosition(LicencePositionCorrection positionCorrection) implements ChangeFormTarget {

    @Override
    public LicenceCorrection correction() {
      return positionCorrection.getLicenceCorrection();
    }

    @Override
    public UUID positionId() {
      return positionCorrection.getPositionId();
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
