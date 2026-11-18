package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.math.BigDecimal;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import uk.co.fivium.formlibrary.validator.decimal.DecimalInputValidator;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationValidationUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class OtherScheduleEventFormValidator {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private final ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  public OtherScheduleEventFormValidator(ScheduleRelativeDateValidationService scheduleRelativeDateValidationService) {
    this.scheduleRelativeDateValidationService = scheduleRelativeDateValidationService;
  }

  boolean isValid(
      OtherScheduleEventForm form,
      Errors errors,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    ValidationUtils.rejectIfEmpty(
        errors,
        "otherScheduleEventCategory",
        "otherScheduleEventCategory.required",
        "Select a category"
    );

    if (form.getOtherScheduleEventCategory() != null
        && form.getOtherScheduleEventCategory().equals(OtherScheduleEventCategory.OTHER_ACTIVITY)) {
      ValidationUtils.rejectIfEmpty(
          errors,
          "otherCategoryName",
          "otherCategoryName.required",
          "Enter a category"
      );
    }

    if (OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT.equals(form.getOtherScheduleEventCategory())) {
      validateRelinquishmentPercentage(form, errors);
    }

    ValidationUtils.rejectIfEmpty(
        errors,
        "description",
        "description.required",
        "Enter the description of the activity"
    );

    ValidationUtils.rejectIfEmpty(
        errors,
        "otherScheduleEventDateOption",
        "otherScheduleEventDateOption.required",
        "Select an option"
    );

    if (form.getOtherScheduleEventDateOption() != null) {
      if (form.getOtherScheduleEventDateOption().equals(OtherScheduleEventDateOption.WITHIN_A_TERM)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceScheduleTermId",
            "licenceScheduleTermId.required",
            "Select a term"
        );
      }

      if (form.getOtherScheduleEventDateOption().equals(OtherScheduleEventDateOption.WITHIN_A_PHASE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "licenceSchedulePhaseId",
            "licenceSchedulePhaseId.required",
            "Select a phase"
        );
      }

      if (form.getOtherScheduleEventDateOption().equals(OtherScheduleEventDateOption.RELATIVE_DATE)) {
        ValidationUtils.rejectIfEmpty(
            errors,
            "relativeEventId",
            "relativeEventId.required",
            "Select an event"
        );

        ThreeFieldDurationValidationUtil.validate(form.getRelativeDuration(), errors);

        if (!errors.hasErrors() && form.getRelativeEventId() != null) {
          scheduleRelativeDateValidationService.validateRelativeDateBeforeEndOfSchedule(
              licenceScheduleDetail,
              form.getRelativeDuration(),
              UUID.fromString(form.getRelativeEventId()),
              errors
          );
        }
      }
    }

    return !errors.hasErrors();
  }

  private void validateRelinquishmentPercentage(OtherScheduleEventForm form, Errors errors) {
    if (StringUtils.isBlank(form.getRelinquishmentPercentage().getInputValue())) {
      errors.rejectValue(
          "relinquishmentPercentage.inputValue",
          "relinquishmentPercentage.required",
          "Enter the mandatory relinquishment percentage"
      );
      return;
    }

    DecimalInputValidator.builder()
        .invalidInputErrorMessage("The percentage must be a number")
        .mustBeBetween(BigDecimal.ZERO, ONE_HUNDRED)
        .mustBeBetweenErrorMessage("The percentage must be between 0 and 100")
        .mustHaveNoMoreThanDecimalPlaces(1)
        .mustHaveNoMoreThanDecimalPlacesErrorMessage("The percentage can only have 1 decimal place")
        .validate(form.getRelinquishmentPercentage(), errors);
  }
}
