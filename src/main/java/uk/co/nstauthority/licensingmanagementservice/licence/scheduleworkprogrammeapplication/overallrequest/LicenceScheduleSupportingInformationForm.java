package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class LicenceScheduleSupportingInformationForm {

  private String licenceProgress;
  private String reasonForAmendment;
  private String planDuringExtension;
  private String impactOnDeliverables;
  private List<UploadedFileForm> documents = new ArrayList<>();

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

  public List<UploadedFileForm> getDocuments() {
    return documents;
  }

  public void setDocuments(List<UploadedFileForm> documents) {
    this.documents = documents;
  }

}