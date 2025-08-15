package uk.co.nstauthority.licensingmanagementservice.components.duration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class ThreeFieldDurationValidationUtilTest {

  @Test
  void validate_valid() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");
    input.setMonths("0");
    input.setDays("0");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void validate_invalid_allFieldsNull() {
    var input = new ThreeFieldDurationInput("testInput", "test");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.years.required",
        "testInput.months.required",
        "testInput.days.required"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.REQUIRED_ERROR_MESSAGE.formatted(input.getFieldDisplayText()),
        "",
        ""
    );
  }

  @Test
  void validate_invalid_MonthsAndDaysFieldsNull() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.months.required",
        "testInput.days.required"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.REQUIRED_ERROR_MESSAGE.formatted(input.getFieldDisplayText()),
        ""
    );
  }

  @Test
  void validate_invalid_dayFieldNull() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");
    input.setMonths("0");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.days.required"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.REQUIRED_ERROR_MESSAGE.formatted(input.getFieldDisplayText())
    );
  }

  @Test
  void validate_invalid_allFieldsNotValidIntegers() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("a");
    input.setMonths("b");
    input.setDays("c");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.years.invalid",
        "testInput.months.invalid",
        "testInput.days.invalid"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.INVALID_ERROR_MESSAGE,
        "",
        ""
    );
  }

  @Test
  void validate_invalid_MonthsAndDaysFieldsNotValidIntegers() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");
    input.setMonths("b");
    input.setDays("c");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.months.invalid",
        "testInput.days.invalid"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.INVALID_ERROR_MESSAGE,
        ""
    );
  }

  @Test
  void validate_invalid_dayFieldNotValidInteger() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");
    input.setMonths("0");
    input.setDays("c");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.days.invalid"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.INVALID_ERROR_MESSAGE
    );
  }

  @Test
  void validate_invalid_allFieldsNegative() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("-1");
    input.setMonths("-1");
    input.setDays("-1");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.years.invalid",
        "testInput.months.invalid",
        "testInput.days.invalid"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.INVALID_ERROR_MESSAGE,
        "",
        ""
    );
  }

  @Test
  void validate_invalid_moreThan11Months() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");
    input.setMonths("12");
    input.setDays("0");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.months.invalid"
    );

    assertMessages(
        bindingResult,
        "The duration must have less than 12 months"
    );
  }

  @Test
  void validate_invalid_moreThan30Days() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("1");
    input.setMonths("0");
    input.setDays("31");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.days.invalid"
    );

    assertMessages(
        bindingResult,
        "The duration must have less than 31 days"
    );
  }

  @Test
  void validate_invalid_durationIsZero() {
    var input = new ThreeFieldDurationInput("testInput", "test");
    input.setYears("0");
    input.setMonths("0");
    input.setDays("0");

    var testForm = new ThreeFieldDurationInputTestForm(input);

    var bindingResult = ValidatorTestingUtil.getBindingResult(testForm);

    ThreeFieldDurationValidationUtil.validate(input, bindingResult);

    assertThat(bindingResult.hasErrors()).isTrue();

    assertCodes(
        bindingResult,
        "testInput.years.invalid",
        "testInput.months.invalid",
        "testInput.days.invalid"
    );

    assertMessages(
        bindingResult,
        ThreeFieldDurationValidationUtil.ZERO_DURATION_ERROR_CODE.formatted(input.getFieldDisplayText()),
        "",
        ""
    );
  }

  private void assertCodes(Errors errors, String... codes) {
    assertThat(errors.getAllErrors()).extracting(DefaultMessageSourceResolvable::getCode)
        .containsExactly(codes);
  }

  private void assertMessages(Errors errors, String... message) {
    assertThat(errors.getAllErrors()).extracting(DefaultMessageSourceResolvable::getDefaultMessage)
        .containsExactly(message);
  }

  private static class ThreeFieldDurationInputTestForm {

    ThreeFieldDurationInput testInput;

    public ThreeFieldDurationInputTestForm(ThreeFieldDurationInput testInput) {
      this.testInput = testInput;
    }

    public ThreeFieldDurationInput getTestInput() {
      return testInput;
    }
  }
}