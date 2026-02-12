package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Audited
@Entity(name = "licence_continuation_other_requirement_request")
public class LicenceContinuationOtherRequirementRequest {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "licence_continuation_application_detail_id")
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  private Boolean financialCapacityEvidenceSubmissionStatus;

  private String actionsToProvideFinancialEvidence;

  private Boolean developmentConsentGrantStatus;

  private String actionsToApproveDevelopmentConsent;

  private Boolean relinquishmentRequirementStatus;

  private String actionsToRelinquishRequiredLicenceArea;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

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

  public LicenceContinuationApplicationDetail getLicenceContinuationApplicationDetail() {
    return licenceContinuationApplicationDetail;
  }

  public void setLicenceContinuationApplicationDetail(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    this.licenceContinuationApplicationDetail = licenceContinuationApplicationDetail;
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
}
