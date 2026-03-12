package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.finaldecision;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.file.FileValidationUtil;

@Service
public class RecordFinalDecisionFormValidator {

  boolean isValid(RecordFinalDecisionForm form, Errors errors) {
    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Provide the decision date")
        .validate(form.getDecisionDate(), errors);

    FileValidationUtil.validator()
        .withMinimumNumberOfFiles(1, "Upload the Final Decision Support Paper")
        .validate(errors, form.getFinalDecisionSupportPapers(), "finalDecisionSupportPapers");

    return !errors.hasErrors();
  }
}
