package uk.co.nstauthority.licensingmanagementservice.mockups.decisionjourney.workprogrammeamendment;

import java.util.ArrayList;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class WorkProgrammeImpactMockupForm {

  private String action;
  private Boolean extendDuration;
  private Boolean amendText;
  private ThreeFieldDurationInput duration = new ThreeFieldDurationInput("duration", "duration");
  private String amendedText;
  private List<String> targetLicences = new ArrayList<>();
  private String transferSelector;

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Boolean getExtendDuration() {
    return extendDuration;
  }

  public void setExtendDuration(Boolean extendDuration) {
    this.extendDuration = extendDuration;
  }

  public Boolean getAmendText() {
    return amendText;
  }

  public void setAmendText(Boolean amendText) {
    this.amendText = amendText;
  }

  public ThreeFieldDurationInput getDuration() {
    return duration;
  }

  public void setDuration(ThreeFieldDurationInput duration) {
    this.duration = duration;
  }

  public String getAmendedText() {
    return amendedText;
  }

  public void setAmendedText(String amendedText) {
    this.amendedText = amendedText;
  }

  public List<String> getTargetLicences() {
    return targetLicences;
  }

  public void setTargetLicences(List<String> targetLicences) {
    this.targetLicences = targetLicences;
  }

  public String getTransferSelector() {
    return transferSelector;
  }

  public void setTransferSelector(String transferSelector) {
    this.transferSelector = transferSelector;
  }
}
