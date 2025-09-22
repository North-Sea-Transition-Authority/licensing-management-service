package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
public class LicenceScheduleExtensionFormValidator {

  boolean isValid(
      LicenceScheduleExtensionForm form,
      Errors errors
  ) {
    ThreeFieldDurationValidationUtil.validate(form.getExtensionDuration(), errors);
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "explanation", "explanation.required",
        "Enter reasons for the extension");
    return !errors.hasErrors();
  }
}