package uk.co.nstauthority.licensingmanagementservice.testharness;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.string.StringInputValidator;

@Component
public class LicencePositionTestHarnessFormValidator {

  public boolean hasErrors(LicencePositionTestHarnessForm form, Errors errors) {
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select a licence")
        .validate(form.getLicenceId(), errors);
    StringInputValidator.builder()
        .emptyInputErrorMessage("Select a second licence")
        .validate(form.getSecondaryLicenceId(), errors);

    if (!errors.hasErrors() && form.getLicenceId().getInputValue().equals(form.getSecondaryLicenceId().getInputValue())) {
      errors.rejectValue(
          "secondaryLicenceId.inputValue",
          "secondaryLicenceId.duplicate",
          "Select a second licence that differs from the first"
      );
    }

    return errors.hasErrors();
  }
}
