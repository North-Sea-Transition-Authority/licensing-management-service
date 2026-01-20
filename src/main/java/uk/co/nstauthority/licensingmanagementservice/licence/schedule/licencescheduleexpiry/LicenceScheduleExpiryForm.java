package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class LicenceScheduleExpiryForm {

  private ThreeFieldDateInput expiryDate = new ThreeFieldDateInput("expiryDate", "expiry date");

  private String comments;

  public ThreeFieldDateInput getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(ThreeFieldDateInput expiryDate) {
    this.expiryDate = expiryDate;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
