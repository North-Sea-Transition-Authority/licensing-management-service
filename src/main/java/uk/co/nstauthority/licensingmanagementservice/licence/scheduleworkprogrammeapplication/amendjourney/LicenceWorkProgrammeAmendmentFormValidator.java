package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
public class LicenceWorkProgrammeAmendmentFormValidator {
  public boolean isValid(LicenceWorkProgrammeAmendmentForm form, Errors errors) {

    if (BooleanUtils.isTrue(form.getDurationExtensionRequired())) {
      ThreeFieldDurationValidationUtil.validate(form.getWorkProgrammeExtensionDuration(), errors);
    }

    if (BooleanUtils.isTrue(form.getAdditionalInfoRequired())) {
      ValidationUtils
          .rejectIfEmptyOrWhitespace(errors, "workProgrammeAmendmentInformation",
              "workProgrammeAmendmentInformation.required",
              "Enter amendment information");
    }

    if (BooleanUtils.isNotTrue(form.getAdditionalInfoRequired())
        && BooleanUtils.isNotTrue(form.getDurationExtensionRequired())) {
      errors.rejectValue("durationExtensionRequired", "durationExtensionRequired.notEmpty",
          "Select if you want to extend or amend the work programme activity");
      errors.rejectValue("additionalInfoRequired", "additionalInfoRequired.notEmpty",
          "");
    }
    return !errors.hasErrors();
  }
}