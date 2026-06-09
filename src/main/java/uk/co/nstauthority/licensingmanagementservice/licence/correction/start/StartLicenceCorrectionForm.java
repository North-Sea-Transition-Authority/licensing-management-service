package uk.co.nstauthority.licensingmanagementservice.licence.correction.start;

import uk.co.fivium.formlibrary.input.StringInput;

public class StartLicenceCorrectionForm {

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
