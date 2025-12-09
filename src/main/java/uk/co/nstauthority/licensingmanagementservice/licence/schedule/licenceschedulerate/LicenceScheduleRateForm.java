package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import uk.co.fivium.formlibrary.input.DecimalInput;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;

public class LicenceScheduleRateForm {

  private RateDefinitionOption rateDefinitionOption;

  private String licenceScheduleTermId;

  private String licenceSchedulePhaseId;

  private RateRelativeDateOption rateRelativeDateOption;

  private ThreeFieldDurationInput relativeDuration = new ThreeFieldDurationInput("relativeDuration", "relative duration");

  private String relativeEventId;

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

  public RateRelativeDateOption getRateRelativeDateOption() {
    return rateRelativeDateOption;
  }

  public void setRateRelativeDateOption(RateRelativeDateOption rateRelativeDateOption) {
    this.rateRelativeDateOption = rateRelativeDateOption;
  }

  public ThreeFieldDurationInput getRelativeDuration() {
    return relativeDuration;
  }

  public void setRelativeDuration(ThreeFieldDurationInput relativeDuration) {
    this.relativeDuration = relativeDuration;
  }

  public String getRelativeEventId() {
    return relativeEventId;
  }

  public void setRelativeEventId(String relativeEventId) {
    this.relativeEventId = relativeEventId;
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
