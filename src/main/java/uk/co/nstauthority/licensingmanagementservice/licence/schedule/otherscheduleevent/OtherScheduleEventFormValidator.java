package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
public class OtherScheduleEventFormValidator {

  boolean isValid(
      OtherScheduleEventForm form,
      Errors errors
  ) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "otherScheduleEventCategory",
        "otherScheduleEventCategory.required",
        "Select a category"
    );

    if (form.getOtherScheduleEventCategory() != null
        && form.getOtherScheduleEventCategory().equals(OtherScheduleEventCategory.OTHER_ACTIVITY)) {
      ValidationUtils.rejectIfEmpty(
          errors,
          "otherCategoryName",
          "otherCategoryName.required",
          "Enter a category"
      );
    }

    ValidationUtils.rejectIfEmpty(
        errors,
        "description",
        "description.required",
        "Enter the description of the activity"
    );

    ValidationUtils.rejectIfEmpty(
        errors,
        "otherScheduleEventDateOption",
        "otherScheduleEventDateOption.required",
        "Select an option"
    );

    if (form.getOtherScheduleEventDateOption() != null) {
      if (form.getOtherScheduleEventDateOption().equals(OtherScheduleEventDateOption.WITHIN_A_TERM)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceScheduleTermId",
            "licenceScheduleTermId.required",
            "Select a term"
        );
      }

      if (form.getOtherScheduleEventDateOption().equals(OtherScheduleEventDateOption.WITHIN_A_PHASE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceSchedulePhaseId",
            "licenceSchedulePhaseId.required",
            "Select a phase"
        );
      }

      if (form.getOtherScheduleEventDateOption().equals(OtherScheduleEventDateOption.RELATIVE_DATE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "relativeEventId",
            "relativeEventId.required",
            "Select an event"
        );

        ThreeFieldDurationValidationUtil.validate(form.getRelativeDuration(), errors);
      }
    }

    return !errors.hasErrors();
  }
}
