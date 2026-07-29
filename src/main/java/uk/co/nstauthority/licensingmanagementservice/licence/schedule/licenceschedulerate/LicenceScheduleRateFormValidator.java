package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleRateFormValidator {

  private final ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  public LicenceScheduleRateFormValidator(ScheduleRelativeDateValidationService scheduleRelativeDateValidationService) {
    this.scheduleRelativeDateValidationService = scheduleRelativeDateValidationService;
  }

  boolean isValid(
      LicenceScheduleRateForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRate licenceScheduleRate
  ) {
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

        if (form.getLicenceScheduleTermId() != null) {
          scheduleRelativeDateValidationService.validateTermRateOverlap(
              licenceScheduleRate,
              licenceScheduleDetail,
              form,
              errors
          );
        }
      }

      if (form.getRateDefinitionOption().equals(RateDefinitionOption.PHASE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceSchedulePhaseId",
            "licenceSchedulePhaseId.required",
            "Select a phase"
        );

        if (form.getLicenceSchedulePhaseId() != null) {
          scheduleRelativeDateValidationService.validatePhaseRateOverlap(
              licenceScheduleRate,
              licenceScheduleDetail,
              form,
              errors
          );
        }
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

        if (form.getRateRelativeDateOption() != null) {
          if (form.getRateRelativeDateOption().equals(RateRelativeDateOption.RELATIVE_TO_START_DATE)) {
            ThreeFieldDurationValidationUtil.validate(form.getRelativeDuration(), errors);

            if (!errors.hasErrors() && form.getRelativeEventId() != null) {
              scheduleRelativeDateValidationService.validateRelativeDateBeforeEndOfSchedule(
                  licenceScheduleDetail,
                  form.getRelativeDuration(),
                  UUID.fromString(form.getRelativeEventId()),
                  errors
              );

              scheduleRelativeDateValidationService.validateRelativeRateOverlap(
                  licenceScheduleRate,
                  licenceScheduleDetail,
                  form,
                  errors
              );
            }
          } else if (form.getRelativeEventId() != null) {
            scheduleRelativeDateValidationService.validateRelativeRateOverlap(
                licenceScheduleRate,
                licenceScheduleDetail,
                form,
                errors
            );
          }
        }
      }
    }

    DecimalInputValidator.builder()
        .emptyInputErrorMessage("Provide the rental rate")
        .invalidInputErrorMessage("The rental rate must be a positive number")
        .mustBeMoreThanOrEqualTo(BigDecimal.ZERO)
        .mustBeMoreThanOrEqualToErrorMessage("The rental rate must be a positive number")
        .mustHaveNoMoreThanDecimalPlaces(2)
        .mustHaveNoMoreThanDecimalPlacesErrorMessage("The rental rate can only include pounds and pence")
        .validate(form.getRentalRate(), errors);

    return !errors.hasErrors();
  }

}
