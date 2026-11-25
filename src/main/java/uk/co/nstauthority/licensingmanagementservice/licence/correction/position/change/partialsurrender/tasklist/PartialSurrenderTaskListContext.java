package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

/**
 * Scopes the task list sections to a partial surrender. Sections are typed against this context rather than
 * {@link LicencePositionStagedPayload} directly so that task lists for other change types staged on the same entity
 * cannot be injected into the partial surrender task list.
 */
public sealed interface PartialSurrenderTaskListContext {

  record Staged(LicencePositionStagedPayload positionStagedPayload) implements PartialSurrenderTaskListContext {
  }

  record LiveChange(LicenceCorrection correction, LicencePosition licencePosition, LicencePositionChange change)
      implements PartialSurrenderTaskListContext {
  }
}
