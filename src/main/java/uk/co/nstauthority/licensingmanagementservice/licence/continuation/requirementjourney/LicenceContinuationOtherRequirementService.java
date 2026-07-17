package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.nstauthority.licensingmanagementservice.file.ApplicationFileService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationOtherRequirementService {

  private final LicenceContinuationOtherRequirementRepository licenceContinuationOtherRequirementRepository;
  private final ApplicationFileService applicationFileService;

  public LicenceContinuationOtherRequirementService(
      LicenceContinuationOtherRequirementRepository licenceContinuationWpaAmendmentRepository,
      ApplicationFileService applicationFileService
  ) {
    this.licenceContinuationOtherRequirementRepository = licenceContinuationWpaAmendmentRepository;
    this.applicationFileService = applicationFileService;
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

    applicationFileService.saveDocuments(
        LicenceContinuationOtherRequirementFileUsages.fromApplication(applicationDetail),
        form.getDocuments()
    );
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
    var form = licenceContinuationOtherRequirementRepository.findByLicenceContinuationApplicationDetail(
        licenceContinuationApplicationDetail
        ).map(this::licenceContinuationOtherRequirementRequestToForm)
        .orElse(new LicenceContinuationOtherRequirementForm());

    var uploadedFileForms = applicationFileService.getUploadedFiles(
            LicenceContinuationOtherRequirementFileUsages.fromApplication(licenceContinuationApplicationDetail))
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();

    form.setDocuments(uploadedFileForms);

    return form;
  }

  public Optional<LicenceContinuationOtherRequirementRequest> getLicenceContinuationApplicationDetail(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return licenceContinuationOtherRequirementRepository.findByLicenceContinuationApplicationDetail(
        licenceContinuationApplicationDetail
    );
  }
}
