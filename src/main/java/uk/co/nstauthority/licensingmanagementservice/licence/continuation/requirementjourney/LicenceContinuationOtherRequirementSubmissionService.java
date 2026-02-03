package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationOtherRequirementSubmissionService {
  private final LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService;
  private final LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator;

  public LicenceContinuationOtherRequirementSubmissionService(
      LicenceContinuationOtherRequirementService licenceContinuationOtherRequirementService,
      LicenceContinuationOtherRequirementValidator licenceContinuationOtherRequirementValidator
  ) {
    this.licenceContinuationOtherRequirementService = licenceContinuationOtherRequirementService;
    this.licenceContinuationOtherRequirementValidator = licenceContinuationOtherRequirementValidator;
  }

  public boolean isSectionSubmittable(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    var form = licenceContinuationOtherRequirementService.getLicenceContinuationOtherRequirementForm(
        licenceContinuationApplicationDetail);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceContinuationOtherRequirementValidator.isValid(form, bindingResult);
  }
}