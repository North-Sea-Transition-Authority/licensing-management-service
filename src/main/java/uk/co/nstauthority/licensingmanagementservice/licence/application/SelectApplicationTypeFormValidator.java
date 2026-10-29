package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectApplicationTypeFormValidator {

  boolean isValid(SelectApplicationTypeForm form, Errors errors, Collection<ApplicationType> availableApplicationTypes) {
    ValidationUtils
        .rejectIfEmpty(errors, "selectedApplicationType", "selectedApplicationType.required", "Select what you are applying for");

    var selectedApplicationType = form.getSelectedApplicationType();
    if (selectedApplicationType != null && !availableApplicationTypes.contains(selectedApplicationType)) {
      errors.rejectValue("selectedApplicationType", "selectedApplicationType.notAvailable", "Select a valid application type");
    }

    return !errors.hasErrors();
  }

}
