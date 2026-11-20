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

    validateFiles(form, errors, "Upload the Final Decision Support Paper");

    return !errors.hasErrors();
  }

  boolean isValidCorrection(UploadDspForm form, Errors errors) {
    if (form.getDspAction() == null) {
      errors.rejectValue("dspAction", "dspAction.required", "Select what you want to do");
    }

    validateFiles(form, errors, "Upload the corrected DSP");

    return !errors.hasErrors();
  }

  private void validateFiles(UploadDspForm form, Errors errors, String missingFilesMessage) {
    FileValidationUtil.validator()
        .withMinimumNumberOfFiles(1, missingFilesMessage)
        .validate(errors, form.getFinalDecisionSupportPapers(), "finalDecisionSupportPapers");
  }
}
