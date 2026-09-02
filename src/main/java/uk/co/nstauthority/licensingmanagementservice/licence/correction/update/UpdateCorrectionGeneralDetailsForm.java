package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;

public class UpdateCorrectionGeneralDetailsForm {

  private final StringInput correctionReference = new StringInput(
      "correctionReference",
      "a correction reference"
  );

  private final StringInput reason = new StringInput("reason", "a reason");

  private String allocatedToWuaId;

  public static UpdateCorrectionGeneralDetailsForm from(LicenceCorrection licenceCorrection) {
    var form = new UpdateCorrectionGeneralDetailsForm();

    form.getCorrectionReference().setInputValue(licenceCorrection.getCorrectionReference());
    form.getReason().setInputValue(licenceCorrection.getReason());

    if (licenceCorrection.getAllocatedToWuaId() != null) {
      form.setAllocatedToWuaId(String.valueOf(licenceCorrection.getAllocatedToWuaId()));
    }

    return form;
  }

  public StringInput getCorrectionReference() {
    return correctionReference;
  }

  public StringInput getReason() {
    return reason;
  }

  public String getAllocatedToWuaId() {
    return allocatedToWuaId;
  }

  public void setAllocatedToWuaId(String allocatedToWuaId) {
    this.allocatedToWuaId = allocatedToWuaId;
  }
}