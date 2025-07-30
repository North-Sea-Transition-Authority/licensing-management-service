package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class ConfirmLicenseePermissionFormValidator {

  boolean isValid(ConfirmLicenseePermissionForm form, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "allLicenseesPermissionConfirmed", "allLicenseesPermissionConfirmed.required",
        "Select whether you have confirmed this request is made on behalf of all licensees");

    if (BooleanUtils.isFalse(form.getAllLicenseesPermissionConfirmed())) {
      errors.rejectValue("allLicenseesPermissionConfirmed", "allLicenseesPermissionConfirmed.required",
          "You must have confirmation from all licensees");
    }

    return !errors.hasErrors();
  }

}
