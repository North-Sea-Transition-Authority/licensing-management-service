package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceWorkProgrammeAmendmentSummaryFormValidator {

  public boolean isValid(
      Errors errors
  ) {

    ValidationUtils.rejectIfEmpty(
        errors,
        "licenceWorkProgrammeAmendmentSummaryOptions",
        "licenceWorkProgrammeAmendmentSummaryOptions.required",
        "Select if you want to add another work programme amendment to this application"
    );

    return !errors.hasErrors();
  }
}