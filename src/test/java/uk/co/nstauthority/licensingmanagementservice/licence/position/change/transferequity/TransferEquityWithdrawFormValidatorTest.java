package uk.co.nstauthority.licensingmanagementservice.licence.position.change.transferequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityWithdrawForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityWithdrawFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.TransferEquityWithdrawalDecision;

class TransferEquityWithdrawFormValidatorTest {

  private static final String REQUIRED_MESSAGE =
      "Select whether the organisation should retain a beneficial interest in the licence";

  private TransferEquityWithdrawFormValidator validator;
  private TransferEquityWithdrawForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    validator = new TransferEquityWithdrawFormValidator();
    form = new TransferEquityWithdrawForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoDecisionSelected_rejectsWithRequiredError() {
    var hasErrors = validator.hasErrors(form, bindingResult);

    assertThat(hasErrors).isTrue();
    assertThat(bindingResult.getFieldErrors("withdrawalDecision"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple("withdrawalDecision.required", REQUIRED_MESSAGE));
  }

  @Test
  void hasErrors_whenDecisionIsNotAValidOption_rejectsWithInvalidError() {
    form.setWithdrawalDecision("NOT_A_DECISION");

    var hasErrors = validator.hasErrors(form, bindingResult);

    assertThat(hasErrors).isTrue();
    assertThat(bindingResult.getFieldErrors("withdrawalDecision"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple("withdrawalDecision.invalid", REQUIRED_MESSAGE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RETAIN", "WITHDRAW"})
  void hasErrors_whenDecisionIsValid_returnsFalse(String decision) {
    form.setWithdrawalDecision(TransferEquityWithdrawalDecision.valueOf(decision).name());

    var hasErrors = validator.hasErrors(form, bindingResult);

    assertThat(hasErrors).isFalse();
    assertThat(bindingResult.hasErrors()).isFalse();
  }
}