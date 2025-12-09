package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;

@Service
public class LicenceScheduleRateFormValidator {

  boolean isValid(LicenceScheduleRateForm form, Errors errors) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "rateDefinitionOption",
        "rateDefinitionOption.required",
        "Select an option"
    );

    if (form.getRateDefinitionOption() != null) {
      if (form.getRateDefinitionOption().equals(RateDefinitionOption.TERM)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceScheduleTermId",
            "licenceScheduleTermId.required",
            "Select a term"
        );
      }

      if (form.getRateDefinitionOption().equals(RateDefinitionOption.PHASE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceSchedulePhaseId",
            "licenceSchedulePhaseId.required",
            "Select a phase"
        );
      }

      if (form.getRateDefinitionOption().equals(RateDefinitionOption.CUSTOM_PERIOD)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "relativeEventId",
            "relativeEventId.required",
            "Select an event"
        );

        ValidationUtils.rejectIfEmpty(
            errors,
            "rateRelativeDateOption",
            "rateRelativeDateOption.required",
            "Select an option"
        );

        if (form.getRateRelativeDateOption() != null
            && form.getRateRelativeDateOption().equals(RateRelativeDateOption.RELATIVE_TO_START_DATE)) {
          ThreeFieldDurationValidationUtil.validate(form.getRelativeDuration(), errors);
        }
      }
    }

    DecimalInputValidator.builder()
        .emptyInputErrorMessage("Provide the rental rate")
        .invalidInputErrorMessage("The rental rate must be a positive number")
        .mustBeMoreThanOrEqualTo(BigDecimal.ZERO)
        .mustBeMoreThanOrEqualToErrorMessage("The rental rate must be a positive number")
        .mustHaveDecimalPlaces(2)
        .mustHaveDecimalPlacesErrorMessage("The rental rate must have 2 decimal places")
        .validate(form.getRentalRate(), errors);

    return !errors.hasErrors();
  }

}
