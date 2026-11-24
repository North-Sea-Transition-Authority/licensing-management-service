package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
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

    validateFiles(
        form.getFinalDecisionSupportPapers(), errors, "finalDecisionSupportPapers", "Upload the Final Decision Support Paper");

    return !errors.hasErrors();
  }

  boolean isValidUpdate(UploadDspForm form, Errors errors) {
    if (form.getDspAction() == null) {
      errors.rejectValue("dspAction", "dspAction.required", "Select what you want to do");
    } else if (form.getDspAction() == UploadDspAction.ADD_ADDENDUM) {
      validateFiles(form.getDspAddendums(), errors, "dspAddendums", "Upload the DSP addendum");
    } else {
      validateFiles(form.getFinalDecisionSupportPapers(), errors, "finalDecisionSupportPapers", "Upload the corrected DSP");
    }

    return !errors.hasErrors();
  }

  private void validateFiles(List<UploadedFileForm> files, Errors errors, String path, String missingFilesMessage) {
    FileValidationUtil.validator()
        .withMinimumNumberOfFiles(1, missingFilesMessage)
        .validate(errors, files, path);
  }
}
