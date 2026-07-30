package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import uk.co.fivium.formlibrary.input.StringInput;

public class CorrectPositionOrderForm {

  private final StringInput positionMove = new StringInput(
      "positionMove",
      "where to move the position"
  );

  public StringInput getPositionMove() {
    return positionMove;
  }

}
