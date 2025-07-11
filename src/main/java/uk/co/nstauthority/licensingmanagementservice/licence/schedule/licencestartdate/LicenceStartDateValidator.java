package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;

@Service
public class LicenceStartDateValidator {

  boolean isValid(LicenceStartDateForm form, Errors errors) {
    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Provide the licence start date")
        .validate(form.getLicenceStartDate(), errors);

    return !errors.hasErrors();
  }

}
