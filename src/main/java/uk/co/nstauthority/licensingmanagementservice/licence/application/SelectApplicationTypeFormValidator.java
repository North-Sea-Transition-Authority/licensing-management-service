package uk.co.nstauthority.licensingmanagementservice.licence.application;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectApplicationTypeFormValidator {

  boolean isValid(Errors errors) {
    ValidationUtils
        .rejectIfEmpty(errors, "selectedApplicationType", "selectedApplicationType.required", "Select what you are applying for");

    return !errors.hasErrors();
  }

}
