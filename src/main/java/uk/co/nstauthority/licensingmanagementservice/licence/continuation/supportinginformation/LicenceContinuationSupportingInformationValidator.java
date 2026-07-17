package uk.co.nstauthority.licensingmanagementservice.licence.continuation.supportinginformation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceContinuationSupportingInformationValidator {

  boolean isValid(LicenceContinuationSupportingInformationForm form, Errors errors) {

    ValidationUtils.rejectIfEmptyOrWhitespace(
        errors,
        "hasAdditionalSupportingInformation",
        "hasAdditionalSupportingInformation.required",
        "Select if you have further supporting information to provide"
    );

    if (BooleanUtils.isTrue(form.getHasAdditionalSupportingInformation()) && form.getDocuments().isEmpty()) {
      errors.rejectValue(
          "documents",
          "documents.required",
          "Upload at least one supporting document"
      );
    }

    return !errors.hasErrors();
  }
}
