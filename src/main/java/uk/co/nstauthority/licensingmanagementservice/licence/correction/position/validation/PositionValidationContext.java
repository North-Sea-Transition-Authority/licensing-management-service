package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public record PositionValidationContext(
    ChronologicalPosition position,
    LicencePositionState resolvedState,
    LicencePositionState previousState,
    boolean isFirstPosition,
    boolean isLastPosition,
    LicenceType licenceType,
    LicenceStatusType licenceStatusType
) {

  public boolean isCarbonStorage() {
    return licenceType == LicenceType.CARBON_STORAGE;
  }

  public boolean isExtantLicence() {
    return licenceStatusType == LicenceStatusType.EXTANT;
  }

}