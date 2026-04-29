package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw.ApplicationWithdrawReasonForm;
import uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw.ApplicationWithdrawReasonValidator;

class ApplicationWithdrawReasonValidatorTest {

  private ApplicationWithdrawReasonValidator validator;
  private ApplicationWithdrawReasonForm form;
  private Errors errors;

  @BeforeEach
  void setUp() {
    validator = new ApplicationWithdrawReasonValidator();
    form = new ApplicationWithdrawReasonForm();
    errors = new BeanPropertyBindingResult(
        form,
        "form"
    );
  }

  @Test
  void isValid_whenReasonIsProvided_returnsTrue() {
    form.setReasonForWithdrawal("We have decided to relinquish instead.");

    boolean result = validator.isValid(errors);

    assertThat(result).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenReasonIsNull_returnsFalse() {
    form.setReasonForWithdrawal(null);

    boolean result = validator.isValid(errors);

    assertThat(result).isFalse();
    assertThat(errors.hasErrors()).isTrue();
  }
}