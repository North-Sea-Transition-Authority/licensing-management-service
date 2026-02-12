package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceContinuationLicenseeInformationFormValidator {

  boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "responsibleOrganisationUnitId", "responsibleOrganisationUnitId.required",
        "Select the licensee for this application");

    return !errors.hasErrors();
  }
}