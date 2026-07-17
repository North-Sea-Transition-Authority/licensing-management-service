package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.ArrayList;
import java.util.List;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class LicenceContinuationOtherRequirementForm {
  private Boolean financialCapacityEvidenceSubmissionStatus;
  private String actionsToProvideFinancialEvidence;
  private Boolean developmentConsentGrantStatus;
  private String actionsToApproveDevelopmentConsent;
  private Boolean relinquishmentRequirementStatus;
  private String actionsToRelinquishRequiredLicenceArea;
  private List<UploadedFileForm> documents = new ArrayList<>();

  public Boolean getFinancialCapacityEvidenceSubmissionStatus() {
    return financialCapacityEvidenceSubmissionStatus;
  }

  public void setFinancialCapacityEvidenceSubmissionStatus(Boolean financialCapacityEvidenceSubmissionStatus) {
    this.financialCapacityEvidenceSubmissionStatus = financialCapacityEvidenceSubmissionStatus;
  }

  public String getActionsToProvideFinancialEvidence() {
    return actionsToProvideFinancialEvidence;
  }

  public void setActionsToProvideFinancialEvidence(String actionsToProvideFinancialEvidence) {
    this.actionsToProvideFinancialEvidence = actionsToProvideFinancialEvidence;
  }

  public Boolean getDevelopmentConsentGrantStatus() {
    return developmentConsentGrantStatus;
  }

  public void setDevelopmentConsentGrantStatus(Boolean developmentConsentGrantStatus) {
    this.developmentConsentGrantStatus = developmentConsentGrantStatus;
  }

  public String getActionsToApproveDevelopmentConsent() {
    return actionsToApproveDevelopmentConsent;
  }

  public void setActionsToApproveDevelopmentConsent(String actionsToApproveDevelopmentConsent) {
    this.actionsToApproveDevelopmentConsent = actionsToApproveDevelopmentConsent;
  }

  public Boolean getRelinquishmentRequirementStatus() {
    return relinquishmentRequirementStatus;
  }

  public void setRelinquishmentRequirementStatus(Boolean relinquishmentRequirementStatus) {
    this.relinquishmentRequirementStatus = relinquishmentRequirementStatus;
  }

  public String getActionsToRelinquishRequiredLicenceArea() {
    return actionsToRelinquishRequiredLicenceArea;
  }

  public void setActionsToRelinquishRequiredLicenceArea(String actionsToRelinquishRequiredLicenceArea) {
    this.actionsToRelinquishRequiredLicenceArea = actionsToRelinquishRequiredLicenceArea;
  }

  public List<UploadedFileForm> getDocuments() {
    return documents;
  }

  public void setDocuments(List<UploadedFileForm> documents) {
    this.documents = documents;
  }
}