package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceContinuationWpaRequirementValidator {

  boolean isValid(LicenceContinuationWpaRequirementForm form, Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "workProgrammeActivitiesCompletionStatus",
        "workProgrammeActivitiesCompletionStatus.required",
        "Select if all work programme activities have been completed"
    );

    if (BooleanUtils.isFalse(form.getWorkProgrammeActivitiesCompletionStatus())) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "actionsToCompleteWorkProgrammeActivities",
          "actionsToCompleteWorkProgrammeActivities.required",
          "Enter the actions being taken to complete any incomplete work programme activities"
      );
    }

    return !errors.hasErrors();
  }
}
