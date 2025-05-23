package uk.co.nstauthority.licensingmanagementservice.xyzapplication.form;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.integer.IntegerInputValidator;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;
import uk.co.nstauthority.licensingmanagementservice.file.FileValidationUtil;

@Service
public class XyzApplicationFormValidator {

  boolean isValid(XyzApplicationForm form, Errors errors) {
    StringInputValidator.builder().validate(form.getApplicationName(), errors);

    IntegerInputValidator.builder()
        .mustBeMoreThanOrEqualTo(1)
        .mustBeLessThanOrEqualTo(100)
        .validate(form.getApplicationNumber(), errors);

    FileValidationUtil.validator()
        .withMinimumNumberOfFiles(1, "Upload at least one application document")
        .withMaximumNumberOfFiles(2, "Upload at most two application documents")
        .validate(errors, form.getDocuments(), "documents");

    ValidationUtils.rejectIfEmptyOrWhitespace(errors,
        "selectedApplication", "selectedApplication.required", "Select the other application");

    return !errors.hasErrors();
  }
}
