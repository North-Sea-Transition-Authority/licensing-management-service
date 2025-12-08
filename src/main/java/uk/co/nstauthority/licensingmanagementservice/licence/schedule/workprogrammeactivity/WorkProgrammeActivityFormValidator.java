package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
public class WorkProgrammeActivityFormValidator {

  boolean isValid(
      WorkProgrammeActivityForm form,
      Errors errors
  ) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "workProgrammeActivityCategory",
        "workProgrammeActivityCategory.required",
        "Select a category"
    );

    if (form.getWorkProgrammeActivityCategory() != null
        && form.getWorkProgrammeActivityCategory().equals(WorkProgrammeActivityCategory.OTHER_ACTIVITY)) {
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
        "workProgrammeActivityCommitment",
        "workProgrammeActivityCommitment.required",
        "Select the commitment of the activity"
    );

    ValidationUtils.rejectIfEmpty(
        errors,
        "workProgrammeActivityDateOption",
        "workProgrammeActivityDateOption.required",
        "Select an option"
    );

    if (form.getWorkProgrammeActivityDateOption() != null) {
      if (form.getWorkProgrammeActivityDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_TERM)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceScheduleTermId",
            "licenceScheduleTermId.required",
            "Select a term"
        );
      }

      if (form.getWorkProgrammeActivityDateOption().equals(WorkProgrammeActivityDateOption.WITHIN_A_PHASE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceSchedulePhaseId",
            "licenceSchedulePhaseId.required",
            "Select a phase"
        );
      }

      if (form.getWorkProgrammeActivityDateOption().equals(WorkProgrammeActivityDateOption.RELATIVE_DATE)) {
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
