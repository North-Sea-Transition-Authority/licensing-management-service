package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import uk.co.fivium.formlibrary.input.DecimalInput;
import uk.co.fivium.formlibrary.input.ThreeFieldDateInput;

public class LicenceScheduleRateForm {

  private RateDefinitionOption rateDefinitionOption;

  private String licenceScheduleTermId;

  private String licenceSchedulePhaseId;

  private ThreeFieldDateInput startDate = new ThreeFieldDateInput("startDate", "Start date");

  private DecimalInput rentalRate = new DecimalInput("rentalRate", "Rental rate");

  private String comments;

  public RateDefinitionOption getRateDefinitionOption() {
    return rateDefinitionOption;
  }

  public void setRateDefinitionOption(RateDefinitionOption rateDefinitionOption) {
    this.rateDefinitionOption = rateDefinitionOption;
  }

  public String getLicenceScheduleTermId() {
    return licenceScheduleTermId;
  }

  public void setLicenceScheduleTermId(String licenceScheduleTermId) {
    this.licenceScheduleTermId = licenceScheduleTermId;
  }

  public String getLicenceSchedulePhaseId() {
    return licenceSchedulePhaseId;
  }

  public void setLicenceSchedulePhaseId(String licenceSchedulePhaseId) {
    this.licenceSchedulePhaseId = licenceSchedulePhaseId;
  }

  public ThreeFieldDateInput getStartDate() {
    return startDate;
  }

  public void setStartDate(ThreeFieldDateInput startDate) {
    this.startDate = startDate;
  }

  public DecimalInput getRentalRate() {
    return rentalRate;
  }

  public void setRentalRate(DecimalInput rentalRate) {
    this.rentalRate = rentalRate;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
