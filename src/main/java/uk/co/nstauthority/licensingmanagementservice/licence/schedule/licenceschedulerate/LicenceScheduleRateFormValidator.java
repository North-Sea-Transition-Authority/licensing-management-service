package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.date.ThreeFieldDateInputValidator;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateService;

@Service
public class LicenceScheduleRateFormValidator {

  private final LicenceStartDateService licenceStartDateService;

  public LicenceScheduleRateFormValidator(LicenceStartDateService licenceStartDateService) {
    this.licenceStartDateService = licenceStartDateService;
  }

  boolean isValid(LicenceScheduleRateForm form, Errors errors, LicenceScheduleDetail licenceScheduleDetail) {
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
        var licenceStartDate = licenceStartDateService.getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

        ThreeFieldDateInputValidator.builder()
            .emptyInputErrorMessage("Provide the start date")
            .mustBeAfterOrEqualTo(licenceStartDate.getStartDate())
            .mustBeAfterOrEqualToErrorMessage("The start date of the rate must be on or after the licence start date")
            .validate(form.getStartDate(), errors);
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
