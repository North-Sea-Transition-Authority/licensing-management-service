package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

class RecordWorkProgrammeAmendmentSummaryFormValidatorTest {

  private final RecordWorkProgrammeAmendmentSummaryFormValidator recordWorkProgrammeAmendmentSummaryFormValidator =
      new RecordWorkProgrammeAmendmentSummaryFormValidator();

  @ParameterizedTest
  @EnumSource(RecordWorkProgrammeAmendmentSummaryOptions.class)
  void isValid_whenOptionSelected_assertNoErrors(RecordWorkProgrammeAmendmentSummaryOptions option) {
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    form.setRecordWorkProgrammeAmendmentSummaryOptions(option);
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentSummaryFormValidator.isValid(bindingResult);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenNothingSelected_assertError() {
    var form = new RecordWorkProgrammeAmendmentSummaryForm();
    var bindingResult = new BeanPropertyBindingResult(form, "form");

    var isValid = recordWorkProgrammeAmendmentSummaryFormValidator.isValid(bindingResult);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "recordWorkProgrammeAmendmentSummaryOptions",
            RecordWorkProgrammeAmendmentSummaryFormValidator.REQUIRED_ERROR_MESSAGE));
  }
}
