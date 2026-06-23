package uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class ExternalContributorFormValidator {

  public boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "addExternalContributors",
        "addExternalContributors.required",
        "Select yes if you want to add external contributors to help with this application"
    );

    return !errors.hasErrors();
  }
}
