package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import uk.co.fivium.formlibrary.input.StringInput;

public class UpdateCorrectionGeneralDetailsForm {

  private final StringInput correctionReference = new StringInput(
      "correctionReference",
      "a correction reference"
  );

  private final StringInput reason = new StringInput("reason", "a reason");

  public StringInput getCorrectionReference() {
    return correctionReference;
  }

  public StringInput getReason() {
    return reason;
  }
}