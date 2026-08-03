package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.setequity.LicencePositionSetEquityFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;

@ExtendWith(MockitoExtension.class)
class LicencePositionSetEquityFormValidatorTest {

  private LicencePositionSetEquityFormValidator validator;
  private LicencePositionSetEquityForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    validator = new LicencePositionSetEquityFormValidator();
    form = new LicencePositionSetEquityForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_emptyForm_reportsBothFields() {
    var hasErrors = validator.hasErrors(form, bindingResult, List.of());

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .contains(
            tuple("transferTo", "Select the organisation equity is being allocated to"),
            tuple("equity.inputValue", "Enter the equity amount this organisation should have as a percentage"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_zeroEquityIsAllowed_forSet() {
    form.setTransferTo("123");
    form.getEquity().setInputValue("0");

    assertThat(validator.hasErrors(form, bindingResult, List.of())).isFalse();
  }
  @ParameterizedTest
  @CsvSource({
      "-1, Equity amount must be 0% or more",
      "100.01, Equity amount must be 100% or less",
      "1.12345678901, Equity amount must have 10 decimal places or fewer"
  })
  void hasErrors_whenEquityInvalid_rejectsEquityFieldWithMessage(String invalidEquity, String expectedMessage) {
    form.setTransferTo("123");
    form.getEquity().setInputValue(invalidEquity);

    validator.hasErrors(form, bindingResult, List.of());
    assertThat(bindingResult.getFieldErrors("equity.inputValue"))
        .extracting(FieldError::getDefaultMessage)
        .containsExactly(expectedMessage);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "50", "100", "33.3333333333"})
  void hasErrors_validForm_returnsFalse(String equity) {
    form.setTransferTo("123");
    form.getEquity().setInputValue(equity);

    assertThat(validator.hasErrors(form, bindingResult, List.of())).isFalse();
  }

  @Test
  void hasErrors_whenTransferToAlreadyHasOperation_rejectsAsDuplicate() {
    form.setTransferTo("123");
    form.getEquity().setInputValue("50");

    var existingOperations = List.of(new SetEquityOperation(123, BigDecimal.valueOf(25)));

    var hasErrors = validator.hasErrors(form, bindingResult, existingOperations);

    assertThat(bindingResult.getFieldErrors("transferTo"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple("transferTo.duplicate", "This organisation has already been added"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenTransferToNotNumeric_rejectsAsInvalid() {
    form.setTransferTo("not-a-number");
    form.getEquity().setInputValue("50");

    var hasErrors = validator.hasErrors(form, bindingResult, List.of());

    assertThat(bindingResult.getFieldErrors("transferTo"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple("transferTo.invalid", "Select the organisation equity is being allocated to"));
    assertThat(hasErrors).isTrue();
  }

}