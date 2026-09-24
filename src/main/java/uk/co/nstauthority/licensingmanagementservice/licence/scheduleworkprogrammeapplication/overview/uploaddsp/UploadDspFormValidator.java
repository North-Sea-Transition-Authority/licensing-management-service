package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.nstauthority.licensingmanagementservice.file.FileValidationUtil;

@Service
public class UploadDspFormValidator {

  private final Clock clock;

  UploadDspFormValidator(Clock clock) {
    this.clock = clock;
  }

  boolean isValid(UploadDspForm form, Errors errors) {
    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Provide the decision date")
        .mustBeBeforeOrEqualTo(LocalDate.now(clock))
        .mustBeBeforeOrEqualToErrorMessage("Decision date must be today or in the past")
        .validate(form.getDecisionDate(), errors);

    FileValidationUtil.validator()
        .withMinimumNumberOfFiles(1, "Upload the Final Decision Support Paper")
        .validate(errors, form.getFinalDecisionSupportPapers(), "finalDecisionSupportPapers");

    return !errors.hasErrors();
  }
}
