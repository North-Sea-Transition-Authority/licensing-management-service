package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

class RecordWorkProgrammeAmendmentDetailsFormValidatorTest {

  private final RecordWorkProgrammeAmendmentDetailsFormValidator recordWorkProgrammeAmendmentDetailsFormValidator =
      new RecordWorkProgrammeAmendmentDetailsFormValidator();

  @Test
  void isValid_whenNoDecisionSelected_assertError() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "decision",
            RecordWorkProgrammeAmendmentDetailsFormValidator.DECISION_REQUIRED_ERROR_MESSAGE));
  }

  @Test
  void isValid_whenWaiveSelected_assertNoErrors() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.WAIVE);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenAcknowledgeSelected_assertNoErrors() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.ACKNOWLEDGE);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenCompleteOnAnotherLicenceSelectedWithNoLicences_assertNoErrors() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.COMPLETE_ON_ANOTHER_LICENCE);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenAmendSelectedWithNeitherDurationNorText_assertError() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "amendDuration",
            RecordWorkProgrammeAmendmentDetailsFormValidator.AMENDMENT_TYPE_REQUIRED_ERROR_MESSAGE));
  }

  @Test
  void isValid_whenAmendDurationSelectedWithNoDuration_assertError() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendDuration(true);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField)
        .containsExactly("amendedDuration.years", "amendedDuration.months", "amendedDuration.days");
  }

  @Test
  void isValid_whenAmendDurationSelectedWithDuration_assertNoErrors() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendDuration(true);
    form.getAmendedDuration().setYears("1");
    form.getAmendedDuration().setMonths("6");
    form.getAmendedDuration().setDays("0");
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenAmendTextSelectedWithNoText_assertError() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendText(true);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "amendedText",
            RecordWorkProgrammeAmendmentDetailsFormValidator.AMENDED_TEXT_REQUIRED_ERROR_MESSAGE));
  }

  @Test
  void isValid_whenAmendTextSelectedWithBlankText_assertError() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendText(true);
    form.setAmendedText("   ");
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "amendedText",
            RecordWorkProgrammeAmendmentDetailsFormValidator.AMENDED_TEXT_REQUIRED_ERROR_MESSAGE));
  }

  @Test
  void isValid_whenBothAmendDurationAndAmendTextSelected_assertBothValidated() {
    var form = new RecordWorkProgrammeAmendmentDetailsForm();
    form.setDecision(WorkProgrammeAmendmentDecision.AMEND);
    form.setAmendDuration(true);
    form.setAmendText(true);
    form.getAmendedDuration().setYears("0");
    form.getAmendedDuration().setMonths("3");
    form.getAmendedDuration().setDays("0");
    form.setAmendedText("The licensee shall drill to 3,500m");
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentDetailsFormValidator.isValid(form, bindingResult);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }
}
