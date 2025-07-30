package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class SelectScheduleWorkProgrammeApplicationLicenceTypeFormValidator {

  boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "selectedLicenceType", "selectedLicenceType.required", "Select a licence type");

    return !errors.hasErrors();
  }

}
