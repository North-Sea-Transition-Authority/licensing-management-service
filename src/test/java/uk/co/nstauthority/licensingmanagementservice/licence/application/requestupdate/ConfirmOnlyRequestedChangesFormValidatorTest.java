package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;

class ConfirmOnlyRequestedChangesFormValidatorTest {

  private final ConfirmOnlyRequestedChangesFormValidator validator = new ConfirmOnlyRequestedChangesFormValidator();

  @Test
  void isValid_whenConfirmed_isValid() {
    var form = new ConfirmOnlyRequestedChangesForm();
    form.setOnlyRequestedChangesMade(true);
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenCheckboxNotTicked_isInvalid() {
    var form = new ConfirmOnlyRequestedChangesForm();
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isFalse();
    assertThat(errors.getFieldError("onlyRequestedChangesMade").getDefaultMessage())
        .isEqualTo("Confirm that you have only made the changes that were requested");
  }

  @Test
  void isValid_whenFalse_isInvalid() {
    var form = new ConfirmOnlyRequestedChangesForm();
    form.setOnlyRequestedChangesMade(false);
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(validator.isValid(form, errors)).isFalse();
  }
}
