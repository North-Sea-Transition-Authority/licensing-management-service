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
    otherRequirementRequest.setLicenceContinuationApplicationDetail(applicationDetail);

    if (BooleanUtils.isTrue(form.getFinancialCapacityEvidenceSubmissionStatus())) {
      otherRequirementRequest.setActionsToProvideFinancialEvidence(null);
    } else {
      otherRequirementRequest.setActionsToProvideFinancialEvidence(form.getActionsToProvideFinancialEvidence());
    }

    licenceContinuationOtherRequirementRepository.save(otherRequirementRequest);
  }

  public LicenceContinuationOtherRequirementForm licenceContinuationOtherRequirementRequestToForm(
      LicenceContinuationOtherRequirementRequest request
  ) {
    LicenceContinuationOtherRequirementForm form = new LicenceContinuationOtherRequirementForm();
    form.setFinancialCapacityEvidenceSubmissionStatus(request.getFinancialCapacityEvidenceSubmissionStatus());
    form.setActionsToProvideFinancialEvidence(request.getActionsToProvideFinancialEvidence());
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
