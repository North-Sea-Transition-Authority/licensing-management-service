package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
class LicenceWorkProgrammeAmendmentFormValidator {
  public boolean isValid(LicenceWorkProgrammeAmendmentForm form, Errors errors) {

    if (form.isDurationExtensionRequired()) {
      ThreeFieldDurationValidationUtil.validate(form.getWorkProgrammeExtensionDuration(), errors);
    }

    if (form.isAdditionalInfoRequired()) {
      ValidationUtils
          .rejectIfEmptyOrWhitespace(errors, "workProgrammeAmendmentInformation",
              "workProgrammeAmendmentInformation.required",
              "Enter amendment information");
    }

    if (!form.isAdditionalInfoRequired() && !form.isDurationExtensionRequired()) {
      errors.rejectValue("durationExtensionRequired", "durationExtensionRequired.notEmpty",
          "Select if you want to extend or amend the work programme activity");

      errors.rejectValue("additionalInfoRequired", "additionalInfoRequired.notEmpty",
          "");
    }

    return !errors.hasErrors();
  }
}