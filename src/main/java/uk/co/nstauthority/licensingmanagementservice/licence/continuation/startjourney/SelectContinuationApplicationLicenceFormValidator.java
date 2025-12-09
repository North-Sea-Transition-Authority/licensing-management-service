package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectContinuationApplicationLicenceFormValidator {

  boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "licenceId", "licenceId.required",
        "Select a licence to create a licence continuation application for");

    return !errors.hasErrors();
  }
}
