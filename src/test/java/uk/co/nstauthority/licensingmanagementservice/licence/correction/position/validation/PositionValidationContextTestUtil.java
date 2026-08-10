package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

public class PositionValidationContextTestUtil {

  private ChronologicalPosition position = ChronologicalPositionTestUtil.newBuilder().build();
  private LicencePositionState previousState = LicencePositionState.EMPTY;
  private boolean isFirstPosition = false;

  public static PositionValidationContextTestUtil newBuilder() {
    return new PositionValidationContextTestUtil();
  }

  public PositionValidationContextTestUtil withPosition(ChronologicalPosition position) {
    this.position = position;
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

  public PositionValidationContext build() {
    return new PositionValidationContext(position, previousState, isFirstPosition);
  }
}
