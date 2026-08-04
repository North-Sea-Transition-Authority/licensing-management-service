package uk.co.nstauthority.licensingmanagementservice.licence;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class EditLicenceDetailsValidator {

  boolean isValid(EditLicenceDetailsForm form, Errors errors) {

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "licenceStatus",
        "licenceStatus.required",
        "Select the status of the licence"
    );

    if (CollectionUtils.isEmpty(form.getOrganisationUnitIds())) {
      errors.rejectValue("organisationUnitSelector", "organisationUnitSelector.notEmpty",
          "You must add at least one licensee");
    }

    return !errors.hasErrors();
  }

}
