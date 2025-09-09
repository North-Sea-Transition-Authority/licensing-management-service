package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;

public class LicenceSchedulePhaseForm {

  private PhaseType phaseType;

  private ThreeFieldDurationInput phaseDuration = new ThreeFieldDurationInput("phaseDuration", "phase");

  private String comments;

  public PhaseType getPhaseType() {
    return phaseType;
  }

  public void setPhaseType(PhaseType phaseType) {
    this.phaseType = phaseType;
  }

  public ThreeFieldDurationInput getPhaseDuration() {
    return phaseDuration;
  }

  public void setPhaseDuration(ThreeFieldDurationInput phaseDuration) {
    this.phaseDuration = phaseDuration;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
