package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceWorkProgrammeAmendmentSummaryFormValidator {

  public boolean isValid(Errors errors) {

    ValidationUtils.rejectIfEmpty(
        errors,
        "licenceWorkProgrammeAmendmentSummaryOptions",
        "licenceWorkProgrammeAmendmentSummaryOptions.required",
        "Select whether you want to add a work programme amendment or not "
    );

    return !errors.hasErrors();
  }
}