package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public interface GeospatialLicenceOperation {

  Set<UUID> featureIds();

  /**
   * Whether every block and subarea this operation acts on is held in the state going into it.
   */
  boolean doInputsMatchPreviousStateOutputs(LicencePositionState licencePositionState);

}
