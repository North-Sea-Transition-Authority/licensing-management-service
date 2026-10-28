package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public class PositionValidationContextTestUtil {

  private ChronologicalPosition position = ChronologicalPositionTestUtil.newBuilder().build();
  private LicencePositionState resolvedState = LicencePositionState.EMPTY;
  private LicencePositionState previousState = LicencePositionState.EMPTY;
  private boolean isFirstPosition = false;
  private boolean isLastPosition = false;
  private LicenceType licenceType = LicenceType.LANDWARD_PRODUCTION;
  private LicenceStatusType licenceStatusType = LicenceStatusType.EXTANT;

  public static PositionValidationContextTestUtil newBuilder() {
    return new PositionValidationContextTestUtil();
  }

  public PositionValidationContextTestUtil withPosition(ChronologicalPosition position) {
    this.position = position;
    return this;
  }

  public PositionValidationContextTestUtil withResolvedState(LicencePositionState resolvedState) {
    this.resolvedState = resolvedState;
    return this;
  }

  public PositionValidationContextTestUtil withPreviousState(LicencePositionState previousState) {
    this.previousState = previousState;
    return this;
  }

  public PositionValidationContextTestUtil withIsFirstPosition(boolean isFirstPosition) {
    this.isFirstPosition = isFirstPosition;
    return this;
  }

  public PositionValidationContextTestUtil withIsLastPosition(boolean isLastPosition) {
    this.isLastPosition = isLastPosition;
    return this;
  }

  public PositionValidationContextTestUtil withLicenceType(LicenceType licenceType) {
    this.licenceType = licenceType;
    return this;
  }

  public PositionValidationContextTestUtil withLicenceStatusType(LicenceStatusType licenceStatusType) {
    this.licenceStatusType = licenceStatusType;
    return this;
  }

  public PositionValidationContext build() {
    return new PositionValidationContext(
        position,
        resolvedState,
        previousState,
        isFirstPosition,
        isLastPosition,
        licenceType,
        licenceStatusType
    );
  }
}