package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenseeInformationFormValidator {

  boolean isValid(LicenseeInformationForm form, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "responsibleOrganisationUnitId", "responsibleOrganisationUnitId.required",
        "Select the licensee for this application");

    ValidationUtils.rejectIfEmpty(errors, "allLicenseesPermissionConfirmed", "allLicenseesPermissionConfirmed.required",
        "Select if you have confirmed this request is made on behalf of all licensees");

    if (BooleanUtils.isFalse(form.getAllLicenseesPermissionConfirmed())) {
      errors.rejectValue("allLicenseesPermissionConfirmed", "allLicenseesPermissionConfirmed.required",
          "You must have confirmation from all licensees");
    }

    return !errors.hasErrors();
  }

}