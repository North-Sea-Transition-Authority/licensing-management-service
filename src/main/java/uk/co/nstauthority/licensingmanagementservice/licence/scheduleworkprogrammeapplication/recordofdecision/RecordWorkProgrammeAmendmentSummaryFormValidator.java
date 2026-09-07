package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class RecordWorkProgrammeAmendmentSummaryFormValidator {

  static final String REQUIRED_ERROR_MESSAGE =
      "Select if you want to add another work programme activity to the scope of this decision";

  public boolean isValid(Errors errors) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "recordWorkProgrammeAmendmentSummaryOptions",
        "recordWorkProgrammeAmendmentSummaryOptions.required",
        REQUIRED_ERROR_MESSAGE
    );

    return !errors.hasErrors();
  }
}
