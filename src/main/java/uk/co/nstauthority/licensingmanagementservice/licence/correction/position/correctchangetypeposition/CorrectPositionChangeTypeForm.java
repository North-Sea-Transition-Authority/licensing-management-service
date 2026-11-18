package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class CorrectPositionChangeTypeForm {

  private final ThreeFieldDateInput correctPositionDate =
      new ThreeFieldDateInput("correctPositionDate", "execution date");

  public ThreeFieldDateInput getCorrectPositionDate() {
    return correctPositionDate;
  }

}
