package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationOtherRequirementService {

  private final LicenceContinuationOtherRequirementRepository licenceContinuationOtherRequirementRepository;

  public LicenceContinuationOtherRequirementService(
      LicenceContinuationOtherRequirementRepository licenceContinuationWpaAmendmentRepository
  ) {
    this.licenceContinuationOtherRequirementRepository = licenceContinuationWpaAmendmentRepository;
  }

  @Transactional
  public void saveLicenceContinuationOtherRequirementForm(
      LicenceContinuationOtherRequirementForm form,
      LicenceContinuationApplicationDetail applicationDetail
  ) {
    var otherRequirementRequest = licenceContinuationOtherRequirementRepository.findByLicenceContinuationApplicationDetail(
        applicationDetail
    ).orElse(new LicenceContinuationOtherRequirementRequest());

    otherRequirementRequest.setFinancialCapacityEvidenceSubmissionStatus(form.getFinancialCapacityEvidenceSubmissionStatus());
    otherRequirementRequest.setDevelopmentConsentGrantStatus(form.getDevelopmentConsentGrantStatus());
    otherRequirementRequest.setLicenceContinuationApplicationDetail(applicationDetail);
    otherRequirementRequest.setRelinquishmentRequirementStatus(form.getRelinquishmentRequirementStatus());

    if (BooleanUtils.isTrue(form.getFinancialCapacityEvidenceSubmissionStatus())) {
      otherRequirementRequest.setActionsToProvideFinancialEvidence(null);
    } else {
      otherRequirementRequest.setActionsToProvideFinancialEvidence(form.getActionsToProvideFinancialEvidence());
    }

    if (BooleanUtils.isTrue(form.getDevelopmentConsentGrantStatus())) {
      otherRequirementRequest.setActionsToApproveDevelopmentConsent(null);
    } else {
      otherRequirementRequest.setActionsToApproveDevelopmentConsent(form.getActionsToApproveDevelopmentConsent());
    }

    if (BooleanUtils.isTrue(form.getRelinquishmentRequirementStatus())) {
      otherRequirementRequest.setActionsToRelinquishRequiredLicenceArea(null);
    } else {
      otherRequirementRequest.setActionsToRelinquishRequiredLicenceArea(form.getActionsToRelinquishRequiredLicenceArea());
    }

    licenceContinuationOtherRequirementRepository.save(otherRequirementRequest);
  }

  public LicenceContinuationOtherRequirementForm licenceContinuationOtherRequirementRequestToForm(
      LicenceContinuationOtherRequirementRequest request
  ) {
    LicenceContinuationOtherRequirementForm form = new LicenceContinuationOtherRequirementForm();
    form.setFinancialCapacityEvidenceSubmissionStatus(request.getFinancialCapacityEvidenceSubmissionStatus());
    form.setActionsToProvideFinancialEvidence(request.getActionsToProvideFinancialEvidence());
    form.setDevelopmentConsentGrantStatus(request.getDevelopmentConsentGrantStatus());
    form.setActionsToApproveDevelopmentConsent(request.getActionsToApproveDevelopmentConsent());
    form.setRelinquishmentRequirementStatus(request.getRelinquishmentRequirementStatus());
    form.setActionsToRelinquishRequiredLicenceArea(request.getActionsToRelinquishRequiredLicenceArea());
    return form;
  }

  public LicenceContinuationOtherRequirementForm getLicenceContinuationOtherRequirementForm(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return  licenceContinuationOtherRequirementRepository.findByLicenceContinuationApplicationDetail(
        licenceContinuationApplicationDetail
        ).map(this::licenceContinuationOtherRequirementRequestToForm)
        .orElse(new LicenceContinuationOtherRequirementForm());
  }
}
