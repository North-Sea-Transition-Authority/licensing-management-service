package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public record PositionValidationContext(
    ChronologicalPosition position,
    LicencePositionState resolvedState,
    LicencePositionState previousState,
    boolean isFirstPosition,
    boolean isCarbonStorage
) {
}