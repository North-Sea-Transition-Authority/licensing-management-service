package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

class CorrectPositionDateForm {

  private final ThreeFieldDateInput correctPositionDate =
      new ThreeFieldDateInput("correctPositionDate", "a position date");

  public ThreeFieldDateInput getCorrectPositionDate() {
    return correctPositionDate;
  }

}