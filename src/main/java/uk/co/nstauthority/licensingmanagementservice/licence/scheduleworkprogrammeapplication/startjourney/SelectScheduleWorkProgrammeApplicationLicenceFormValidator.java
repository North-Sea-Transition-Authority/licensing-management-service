package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectScheduleWorkProgrammeApplicationLicenceFormValidator {

  boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "licenceId", "licenceId.required",
        "Select a licence to create an schedule extension or work programme amendment application application for");

    return !errors.hasErrors();
  }

}
