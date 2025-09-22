package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class LicenceScheduleExtensionForm {

  private ThreeFieldDurationInput extensionDuration = new ThreeFieldDurationInput("extensionDuration",
      "extension");

  private String explanation;

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public ThreeFieldDurationInput getExtensionDuration() {
    return extensionDuration;
  }

  public void setExtensionDuration(
      ThreeFieldDurationInput extensionDuration) {
    this.extensionDuration = extensionDuration;
  }
}