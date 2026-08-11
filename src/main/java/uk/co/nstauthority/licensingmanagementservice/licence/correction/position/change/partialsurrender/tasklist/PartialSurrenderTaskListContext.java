package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;

/**
 * Scopes the task list sections to a partial surrender. Sections are typed against this context rather than
 * {@link LicencePositionCorrection} directly so that task lists for other change types staged on the same entity
 * cannot be injected into the partial surrender task list.
 */
public record PartialSurrenderTaskListContext(LicencePositionCorrection positionCorrection) {
}
