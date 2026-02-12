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
          "Enter actions being taken to provide evidence of financial capacity to the NSTA Finance Team"
      );
    }

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "developmentConsentGrantStatus",
        "developmentConsentGrantStatus.required",
        "Select if Development Consent has been granted"
    );

    if (BooleanUtils.isFalse(form.getDevelopmentConsentGrantStatus())) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "actionsToApproveDevelopmentConsent",
          "actionsToApproveDevelopmentConsent.required",
          "Enter actions being taken to get the Development Consent approved"
      );
    }

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "relinquishmentRequirementStatus",
        "relinquishmentRequirementStatus.required",
        "Select if required amount of the licensed area has been relinquished"
    );

    if (BooleanUtils.isFalse(form.getRelinquishmentRequirementStatus())) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "actionsToRelinquishRequiredLicenceArea",
          "actionsToRelinquishRequiredLicenceArea.required",
          "Enter the actions being taken to relinquish the required amount of the licence area"
      );
    }

    return !errors.hasErrors();
  }
}
