package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;

@Service
public class LicenceContinuationWpaSubmissionService {
  private final LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService;
  private final LicenceContinuationWpaRequirementValidator licenceContinuationWpaRequirementValidator;

  public LicenceContinuationWpaSubmissionService(
      LicenceContinuationWpaRequirementService licenceContinuationWpaRequirementService,
      LicenceContinuationWpaRequirementValidator licenceContinuationWpaRequirementValidator
  ) {
    this.licenceContinuationWpaRequirementService = licenceContinuationWpaRequirementService;
    this.licenceContinuationWpaRequirementValidator = licenceContinuationWpaRequirementValidator;
  }

  public boolean isSectionSubmittable(LicenceContinuationApplicationDetail licenceContinuationApplicationDetail) {
    var form = licenceContinuationWpaRequirementService.getLicenceContinuationWorkProgrammeActivitiesRequirementForm(
        licenceContinuationApplicationDetail);
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
    return licenceContinuationWpaRequirementValidator.isValid(form, bindingResult);
  }
}