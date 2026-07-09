package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;

@Service
public class LicenceWorkProgrammeAmendmentFormValidator {
  public boolean isValid(LicenceWorkProgrammeAmendmentForm form, Errors errors, WorkProgrammeActivity workProgrammeActivity) {
    var linkedToRelativeDate = workProgrammeActivity.getDateOption().equals(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    var extensionRequested = BooleanUtils.isTrue(form.getDurationExtensionRequired());
    if (!linkedToRelativeDate) {
      form.setAdditionalInfoRequired(true);
    }
    var additionalInfoRequested = BooleanUtils.isTrue(form.getAdditionalInfoRequired());

    if (extensionRequested) {
      ThreeFieldDurationValidationUtil.validate(form.getWorkProgrammeExtensionDuration(), errors);
    }

    if (additionalInfoRequested) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "workProgrammeAmendmentInformation",
          "workProgrammeAmendmentInformation.required",
          "Enter the amendments would you like to request");
    }

    if ((!extensionRequested && !additionalInfoRequested)) {
      errors.rejectValue("durationExtensionRequired", "durationExtensionRequired.notEmpty",
          "Select if you want to extend the work programme activity completion date");
      errors.rejectValue("additionalInfoRequired", "additionalInfoRequired.notEmpty",
          "Select if you want to amend the work programme activity content");
    }

    return !errors.hasErrors();
  }
}