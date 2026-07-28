package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.nstauthority.licensingmanagementservice.file.FileValidationUtil;

@Service
public class LicenceContinuationSupportingInformationValidator {

  boolean isValid(LicenceContinuationSupportingInformationForm form, Errors errors) {

    var minimumNumberOfFiles = BooleanUtils.isTrue(form.getHasAdditionalSupportingInformation()) ? 1 : 0;

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "hasAdditionalSupportingInformation",
        "hasAdditionalSupportingInformation.required",
        "Select if you have further supporting information to provide"
    );

    FileValidationUtil.validator()
        .withMandatoryDescriptions(true)
        .withMinimumNumberOfFiles(minimumNumberOfFiles, "Upload at least one supporting document")
        .validate(errors, form.getDocuments(), "documents");

    return !errors.hasErrors();
  }
}
