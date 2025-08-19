package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.requestpurpose;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class SwpApplicationRequestPurposeValidator {

  boolean isValid(SwpApplicationRequestPurposeForm form, Errors errors) {

    if (form.getRequestPurposes().isEmpty()) {
      errors.rejectValue("requestPurposes", "requestPurposes.empty", "Select what you are requesting to do");
    }

    return !errors.hasErrors();
  }
}
