package uk.co.nstauthority.licensingmanagementservice.testharness;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Service
@Profile("test-harness")
class LicencePositionFeatureTestHarnessFormValidator {

  private static final String LICENCE_ID_FIELD = "licenceId.inputValue";

  public boolean hasErrors(
      LicencePositionFeatureTestHarnessForm form,
      Errors errors,
      LicencePositionFeatureSeedState seedState
  ) {
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select a licence")
        .validate(form.getLicenceId(), errors);

    if (seedState == null) {
      return errors.hasErrors();
    }

    if (seedState.positionCount() == 0) {
      errors.rejectValue(
          LICENCE_ID_FIELD,
          "licenceId.noPositions",
          "Select a licence that has positions to link features to"
      );
    } else if (seedState.hasLinkedFeatures()) {
      errors.rejectValue(
          LICENCE_ID_FIELD,
          "licenceId.hasLinkedFeatures",
          "Select a licence that has no features linked to its positions yet"
      );
    }

    return errors.hasErrors();
  }
}
