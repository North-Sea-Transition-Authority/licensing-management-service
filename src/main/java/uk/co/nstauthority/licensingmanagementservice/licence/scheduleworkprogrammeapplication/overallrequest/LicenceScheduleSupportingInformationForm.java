package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

public class LicenceScheduleSupportingInformationForm {

  private String licenceProgress;
  private String reasonForAmendment;
  private String planDuringExtension;
  private String impactOnDeliverables;

  public String getLicenceProgress() {
    return licenceProgress;
  }

  public void setLicenceProgress(String licenceProgress) {
    this.licenceProgress = licenceProgress;
  }

  public String getReasonForAmendment() {
    return reasonForAmendment;
  }

  public void setReasonForAmendment(String reasonForAmendment) {
    this.reasonForAmendment = reasonForAmendment;
  }

  public String getPlanDuringExtension() {
    return planDuringExtension;
  }

  public void setPlanDuringExtension(String planDuringExtension) {
    this.planDuringExtension = planDuringExtension;
  }

  public String getImpactOnDeliverables() {
    return impactOnDeliverables;
  }

  public void setImpactOnDeliverables(String impactOnDeliverables) {
    this.impactOnDeliverables = impactOnDeliverables;
  }
}