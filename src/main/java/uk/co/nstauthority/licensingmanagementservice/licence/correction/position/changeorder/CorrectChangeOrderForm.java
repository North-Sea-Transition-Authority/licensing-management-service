package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import uk.co.fivium.formlibrary.input.StringInput;

public class CorrectChangeOrderForm {

  private final StringInput changeMove = new StringInput(
      "changeMove",
      "where to move the change"
  );

  public StringInput getChangeMove() {
    return changeMove;
  }

}