package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;

public class LicenceScheduleTermForm {

  private TermType termType;

  private ThreeFieldDurationInput termDuration = new ThreeFieldDurationInput("termDuration", "term");

  private String comments;

  public TermType getTermType() {
    return termType;
  }

  public void setTermType(TermType termType) {
    this.termType = termType;
  }

  public ThreeFieldDurationInput getTermDuration() {
    return termDuration;
  }

  public void setTermDuration(ThreeFieldDurationInput termDuration) {
    this.termDuration = termDuration;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
