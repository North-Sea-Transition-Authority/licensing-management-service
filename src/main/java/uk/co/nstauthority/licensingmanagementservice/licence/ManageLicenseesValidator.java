package uk.co.nstauthority.licensingmanagementservice.licence;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;

@Service
public class ManageLicenseesValidator {

  boolean isValid(ManageLicenseesForm form, Errors errors) {

    if (CollectionUtils.isEmpty(form.getOrganisationUnitIds())) {
      errors.rejectValue("organisationUnitSelector", "organisationUnitSelector.notEmpty",
          "You must add at least one licensee");
    }

    return !errors.hasErrors();
  }

}
