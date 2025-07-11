package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class LicenceStartDateForm {

  private ThreeFieldDateInput licenceStartDate = new ThreeFieldDateInput("licenceStartDate", "licence start date");

  public ThreeFieldDateInput getLicenceStartDate() {
    return licenceStartDate;
  }

  public void setLicenceStartDate(ThreeFieldDateInput licenceStartDate) {
    this.licenceStartDate = licenceStartDate;
  }
}
