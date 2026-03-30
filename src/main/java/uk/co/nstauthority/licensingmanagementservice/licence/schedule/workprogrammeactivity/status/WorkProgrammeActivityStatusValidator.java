package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;


@Service
public class WorkProgrammeActivityStatusValidator {

  public boolean isValid(WorkProgrammeActivityStatusForm form, Errors errors) {
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

    return !errors.hasErrors();
  }

}
