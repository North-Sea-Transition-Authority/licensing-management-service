package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class LicenceWorkProgrammeAmendmentForm {

  private Boolean durationExtensionRequired;
  private Boolean additionalInfoRequired;

  private ThreeFieldDurationInput workProgrammeExtensionDuration = new ThreeFieldDurationInput(
      "workProgrammeExtensionDuration", "extension");
  private String workProgrammeAmendmentInformation;

  public ThreeFieldDurationInput getWorkProgrammeExtensionDuration() {
    return workProgrammeExtensionDuration;
  }

  public void setWorkProgrammeExtensionDuration(
      ThreeFieldDurationInput workProgrammeExtensionDuration) {
    this.workProgrammeExtensionDuration = workProgrammeExtensionDuration;
  }

  public String getWorkProgrammeAmendmentInformation() {
    return workProgrammeAmendmentInformation;
  }

  public void setWorkProgrammeAmendmentInformation(String workProgrammeAmendmentInformation) {
    this.workProgrammeAmendmentInformation = workProgrammeAmendmentInformation;
  }

  public Boolean getDurationExtensionRequired() {
    return durationExtensionRequired;
  }

  public void setDurationExtensionRequired(Boolean durationExtensionRequired) {
    this.durationExtensionRequired = durationExtensionRequired;
  }

  public Boolean getAdditionalInfoRequired() {
    return additionalInfoRequired;
  }

  public void setAdditionalInfoRequired(Boolean additionalInfoRequired) {
    this.additionalInfoRequired = additionalInfoRequired;
  }
}