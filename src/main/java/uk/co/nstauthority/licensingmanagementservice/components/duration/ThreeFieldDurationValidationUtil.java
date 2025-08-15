package uk.co.nstauthority.licensingmanagementservice.components.duration;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.validation.Errors;

public class ThreeFieldDurationValidationUtil {

  static final String REQUIRED_ERROR_MESSAGE = "Provide the %s duration";
  static final String INVALID_ERROR_MESSAGE = "The duration must be a number";
  static final String ZERO_DURATION_ERROR_CODE = "The %s duration must be at least 1 day";
  private static final String YEAR_FIELD_SUFFIX = ".years";
  private static final String MONTH_FIELD_SUFFIX = ".months";
  private static final String DAY_FIELD_SUFFIX = ".days";
  private static final String REQUIRED_ERROR_CODE = ".required";
  private static final String INVALID_ERROR_CODE = ".invalid";
  private static final String BLANK_ERROR_MESSAGE = "%s%s%s";

  private ThreeFieldDurationValidationUtil() {
  }

  public static void validate(ThreeFieldDurationInput input, Errors errors) {
    var fieldName = input.getFieldName();

    if (ObjectUtils.anyNull(input.getYears(), input.getMonths(), input.getDays())) {
      rejectFieldsAndSetMessage(
          fieldName,
          REQUIRED_ERROR_MESSAGE.formatted(input.getFieldDisplayText()),
          input.getYears() == null ? BLANK_ERROR_MESSAGE.formatted(fieldName, YEAR_FIELD_SUFFIX, REQUIRED_ERROR_CODE) : null,
          input.getMonths() == null ? BLANK_ERROR_MESSAGE.formatted(fieldName, MONTH_FIELD_SUFFIX, REQUIRED_ERROR_CODE) : null,
          input.getDays() == null ? BLANK_ERROR_MESSAGE.formatted(fieldName, DAY_FIELD_SUFFIX, REQUIRED_ERROR_CODE) : null,
          errors
      );
      return;
    }

    var yearsValid = isValidNumber(input.getYears());
    var monthsValid = isValidNumber(input.getMonths());
    var daysValid = isValidNumber(input.getDays());

    if (!yearsValid || !monthsValid || !daysValid) {
      rejectFieldsAndSetMessage(
          fieldName,
          INVALID_ERROR_MESSAGE,
          !yearsValid ? BLANK_ERROR_MESSAGE.formatted(fieldName, YEAR_FIELD_SUFFIX, INVALID_ERROR_CODE) : null,
          !monthsValid ? BLANK_ERROR_MESSAGE.formatted(fieldName, MONTH_FIELD_SUFFIX, INVALID_ERROR_CODE) : null,
          !daysValid ? BLANK_ERROR_MESSAGE.formatted(fieldName, DAY_FIELD_SUFFIX, INVALID_ERROR_CODE) : null,
          errors
      );
      return;
    }

    var years = Integer.parseInt(input.getYears());
    var months = Integer.parseInt(input.getMonths());
    var days = Integer.parseInt(input.getDays());

    if (Integer.parseInt(input.getMonths()) >= 12) {
      errors.rejectValue(
          fieldName + MONTH_FIELD_SUFFIX,
          BLANK_ERROR_MESSAGE.formatted(fieldName, MONTH_FIELD_SUFFIX, INVALID_ERROR_CODE),
          "The duration must have less than 12 months"
      );
      return;
    }

    if (Integer.parseInt(input.getDays()) >= 31) {
      errors.rejectValue(
          fieldName + DAY_FIELD_SUFFIX,
          BLANK_ERROR_MESSAGE.formatted(fieldName, DAY_FIELD_SUFFIX, INVALID_ERROR_CODE),
          "The duration must have less than 31 days"
      );
      return;
    }

    if (years < 1 && months < 1 && days < 1) {
      rejectFieldsAndSetMessage(
          fieldName,
          ZERO_DURATION_ERROR_CODE.formatted(input.getFieldDisplayText()),
          BLANK_ERROR_MESSAGE.formatted(fieldName, YEAR_FIELD_SUFFIX, INVALID_ERROR_CODE),
          BLANK_ERROR_MESSAGE.formatted(fieldName, MONTH_FIELD_SUFFIX, INVALID_ERROR_CODE),
          BLANK_ERROR_MESSAGE.formatted(fieldName, DAY_FIELD_SUFFIX, INVALID_ERROR_CODE),
          errors
      );
    }
  }

  private static boolean isValidNumber(String inputValue) {
    try {
      var number = Integer.parseInt(inputValue);
      if (number < 0) {
        return false;
      }
    } catch (NumberFormatException e) {
      return false;
    }

    return true;
  }

  /**
   * Rejects the year/month/day fields with the given codes and adds the error message to the 'first' rejected field for
   * compatibility with FDS.
   * A null code indicates that field should not be rejected.
   * At least one of the year, month or day codes must be not null.
   *
   * @param fieldPrefix    The prefix of the form date fields. e.g: proposedStartDay has a prefix of proposedStart
   * @param errorMessage   The error message to attach to the first field with an error
   * @param dayErrorCode   An error code string to attach to the day field. This should be null if the day field has no error
   * @param monthErrorCode An error code string to attach to the month field. This should be null if the month field has no error
   * @param yearErrorCode  An error code string to attach to the year field. This should be null if the year field has no error
   * @param errors         THe Errors object to add rejection codes and messages to
   */
  private static void rejectFieldsAndSetMessage(String fieldPrefix,
                                                String errorMessage,
                                                String yearErrorCode,
                                                String monthErrorCode,
                                                String dayErrorCode,
                                                Errors errors) {
    if (ObjectUtils.allNull(dayErrorCode, monthErrorCode, yearErrorCode)) {
      throw new IllegalArgumentException("At least one of year, month or day codes must be provided.");
    }

    if (yearErrorCode != null) {
      errors.rejectValue(
          fieldPrefix + YEAR_FIELD_SUFFIX,
          yearErrorCode,
          errorMessage
      );
    }

    if (monthErrorCode != null) {
      if (yearErrorCode == null) {
        errors.rejectValue(
            fieldPrefix + MONTH_FIELD_SUFFIX,
            monthErrorCode,
            errorMessage
        );
      } else {
        errors.rejectValue(
            fieldPrefix + MONTH_FIELD_SUFFIX,
            monthErrorCode,
            ""
        );
      }
    }

    if (dayErrorCode != null) {
      if (yearErrorCode == null && monthErrorCode == null) {
        errors.rejectValue(
            fieldPrefix + DAY_FIELD_SUFFIX,
            dayErrorCode,
            errorMessage
        );
      } else {
        errors.rejectValue(
            fieldPrefix + DAY_FIELD_SUFFIX,
            dayErrorCode,
            ""
        );
      }
    }
  }

}
