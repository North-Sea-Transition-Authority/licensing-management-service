package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class CorrectPositionChangeTypeForm {

  public static final String OTHER_DATE_OPTION = "OTHER_DATE";

  private final StringInput changeTypePositionMove = new StringInput(
      "changeTypePositionMove",
      "where to move the change type position"
  );

  private final ThreeFieldDateInput correctPositionDate =
      new ThreeFieldDateInput("correctPositionDate", "position date");

  public StringInput getChangeTypePositionMove() {
    return changeTypePositionMove;
  }

  public ThreeFieldDateInput getCorrectPositionDate() {
    return correctPositionDate;
  }

}
