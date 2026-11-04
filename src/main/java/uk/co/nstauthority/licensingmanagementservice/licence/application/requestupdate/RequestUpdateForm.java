package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class RequestUpdateForm {

  private ThreeFieldDateInput updateDeadlineDate = new ThreeFieldDateInput("updateDeadlineDate", "update deadline date");
  private String updateReason;

  public ThreeFieldDateInput getUpdateDeadlineDate() {
    return updateDeadlineDate;
  }

  public void setUpdateDeadlineDate(ThreeFieldDateInput updateDeadlineDate) {
    this.updateDeadlineDate = updateDeadlineDate;
  }

  public String getUpdateReason() {
    return updateReason;
  }

  public void setUpdateReason(String updateReason) {
    this.updateReason = updateReason;
  }
}
