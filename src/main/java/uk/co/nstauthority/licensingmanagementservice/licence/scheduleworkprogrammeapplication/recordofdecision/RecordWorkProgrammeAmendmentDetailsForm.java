package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class RecordWorkProgrammeAmendmentDetailsForm {

  private WorkProgrammeAmendmentDecision decision;

  private Boolean amendDuration;

  private Boolean amendText;

  private ThreeFieldDurationInput amendedDuration =
      new ThreeFieldDurationInput("amendedDuration", "amended duration");

  private String amendedText;

  private List<String> targetLicenceIds = new ArrayList<>();

  private String targetLicenceSelector;

  public WorkProgrammeAmendmentDecision getDecision() {
    return decision;
  }

  public void setDecision(WorkProgrammeAmendmentDecision decision) {
    this.decision = decision;
  }

  public Boolean getAmendDuration() {
    return amendDuration;
  }

  public void setAmendDuration(Boolean amendDuration) {
    this.amendDuration = amendDuration;
  }

  public Boolean getAmendText() {
    return amendText;
  }

  public void setAmendText(Boolean amendText) {
    this.amendText = amendText;
  }

  public ThreeFieldDurationInput getAmendedDuration() {
    return amendedDuration;
  }

  public void setAmendedDuration(ThreeFieldDurationInput amendedDuration) {
    this.amendedDuration = amendedDuration;
  }

  public String getAmendedText() {
    return amendedText;
  }

  public void setAmendedText(String amendedText) {
    this.amendedText = amendedText;
  }

  public List<String> getTargetLicenceIds() {
    return targetLicenceIds;
  }

  public void setTargetLicenceIds(List<String> targetLicenceIds) {
    this.targetLicenceIds = targetLicenceIds;
  }

  public String getTargetLicenceSelector() {
    return targetLicenceSelector;
  }

  public void setTargetLicenceSelector(String targetLicenceSelector) {
    this.targetLicenceSelector = targetLicenceSelector;
  }
}
