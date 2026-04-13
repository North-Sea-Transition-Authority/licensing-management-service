package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Service
public class LicenceContinuationLicenceOperatorsValidator {

  boolean isValid(Errors errors,
                  boolean hasMissingOperators
  ) {
    if (BooleanUtils.isTrue(hasMissingOperators)) {
      ValidationUtils.rejectIfEmptyOrWhitespace(
          errors,
          "pendingActionsExplanation",
          "pendingActionsExplanation.required",
          "Enter what actions are being taken to assign an operator"
      );
    }

    return !errors.hasErrors();
  }
}