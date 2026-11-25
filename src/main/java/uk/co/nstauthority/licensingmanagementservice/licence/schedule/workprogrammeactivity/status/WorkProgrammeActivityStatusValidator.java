package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;


@Service
public class WorkProgrammeActivityStatusValidator {

  public boolean isValid(
      WorkProgrammeActivityStatusForm form,
      WorkProgrammeActivity workProgrammeActivity,
      Errors errors
  ) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "status",
        "status.required",
        "Select the status of the work programme activity"
    );

    if (form.getStatus() != null && form.getStatus().equals(WorkProgrammeStatus.TRANSFERRED)) {
      ValidationUtils.rejectIfEmpty(
          errors,
          "transferredToLicenceId",
          "transferredToLicenceId.required",
          "Select the licence that the work programme activity has been transferred to"
      );
    }

    if (form.getStatus() != null && !form.getStatus().isAvailableFor(workProgrammeActivity.getCategory())) {
      errors.rejectValue(
          "status",
          "status.invalid",
          "Select a status that applies to this work programme activity"
      );
    }

    return !errors.hasErrors();
  }

}
