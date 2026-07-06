package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import java.time.LocalDate;
import java.time.Month;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;

@Service
public class LicenceStartDateValidator {

  boolean isValid(LicenceStartDateForm form, Errors errors) {
    ThreeFieldDateInputValidator.builder()
        .emptyInputErrorMessage("Provide the licence start date")
        .mustBeAfterDate(LocalDate.of(1900, Month.JANUARY, 1))
        .mustBeAfterDateErrorMessage("The date must be after 1 January 1900")
        .validate(form.getLicenceStartDate(), errors);

    return !errors.hasErrors();
  }

}
