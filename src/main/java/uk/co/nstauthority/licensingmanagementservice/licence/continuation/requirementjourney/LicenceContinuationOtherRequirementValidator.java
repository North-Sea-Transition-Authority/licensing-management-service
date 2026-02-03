package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceContinuationOtherRequirementValidator {

  boolean isValid(LicenceContinuationOtherRequirementForm form, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "financialCapacityEvidenceSubmissionStatus",
        "financialCapacityEvidenceSubmissionStatus.required",
        "Select if evidence of financial capacity have been submitted"
    );

    if (BooleanUtils.isFalse(form.getFinancialCapacityEvidenceSubmissionStatus())) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "actionsToProvideFinancialEvidence",
          "actionsToProvideFinancialEvidence.required",
          "Enter the actions being taken to provide evidence of financial capacity to the NSTA Finance Team"
      );
    }

    return !errors.hasErrors();
  }
}
