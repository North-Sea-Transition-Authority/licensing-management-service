package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

class AddLicencePositionCorrectionForm {

  private final StringInput correctionReference = new StringInput(
      "correctionReference",
      "a correction reference"
  );

  private final ThreeFieldDateInput positionDate = new ThreeFieldDateInput("positionDate", "a position date");

  public StringInput getCorrectionReference() {
    return correctionReference;
  }

  public ThreeFieldDateInput getPositionDate() {
    return positionDate;
  }

}