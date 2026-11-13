package uk.co.nstauthority.licensingmanagementservice.licence.position.change.transferequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.transferequity.LicencePositionTransferEquityFormValidator;

@ExtendWith(MockitoExtension.class)
class LicencePositionTransferEquityFormValidatorTest {

  private static final int TRANSFER_FROM = 123;
  private static final int TRANSFER_TO = 456;
  private static final BigDecimal TRANSFER_TO_EQUITY = new BigDecimal("10");
  private static final Map<Integer, BigDecimal> SUFFICIENT_HOLDINGS = Map.of(
      TRANSFER_FROM, new BigDecimal("100"),
      TRANSFER_TO, TRANSFER_TO_EQUITY
  );

  private LicencePositionTransferEquityFormValidator validator;
  private LicencePositionTransferEquityForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    validator = new LicencePositionTransferEquityFormValidator();
    form = new LicencePositionTransferEquityForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_emptyForm_reportsAllFields() {
    var hasErrors = validator.hasErrors(form, bindingResult, Map.of());

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .contains(
            tuple("transferFrom", "Select the organisation equity is being transferred from"),
            tuple("transferTo", "Select the organisation equity is being transferred to"),
            tuple("equity.inputValue", "Enter the equity amount being transferred as a percentage"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenTransferToSameAsTransferFrom_rejectsTransferTo() {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_FROM));
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(form, bindingResult, SUFFICIENT_HOLDINGS);

    assertThat(bindingResult.getFieldErrors("transferTo"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "transferTo.sameAsTransferFrom",
            "The organisation equity is transferred to must be different from the organisation it is transferred from"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenEquityIsZero_rejectsEquity() {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("0");

    validator.hasErrors(form, bindingResult, SUFFICIENT_HOLDINGS);

    assertThat(bindingResult.getFieldErrors("equity.inputValue"))
        .extracting(FieldError::getDefaultMessage)
        .containsExactly("Equity amount must be more than 0%");
  }

  @ParameterizedTest
  @CsvSource({
      "-1, Equity amount must be more than 0%",
      "0, Equity amount must be more than 0%",
      "100.01, Equity amount must be 100% or less",
      "1.12345678901, Equity amount must have 10 decimal places or fewer"
  })
  void hasErrors_whenEquityInvalid_rejectsEquityFieldWithMessage(String invalidEquity, String expectedMessage) {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue(invalidEquity);

    validator.hasErrors(form, bindingResult, SUFFICIENT_HOLDINGS);

    assertThat(bindingResult.getFieldErrors("equity.inputValue"))
        .extracting(FieldError::getDefaultMessage)
        .containsExactly(expectedMessage);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.0000000001", "50", "100", "33.3333333333"})
  void hasErrors_whenFormValid_returnsFalse(String equity) {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue(equity);

    assertThat(validator.hasErrors(form, bindingResult, SUFFICIENT_HOLDINGS)).isFalse();
  }

  @Test
  void hasErrors_whenTransferorHoldsNoEquity_rejectsTransferFrom() {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(form, bindingResult, Map.of(TRANSFER_TO, TRANSFER_TO_EQUITY));

    assertThat(bindingResult.getFieldErrors("transferFrom"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("transferFrom.insufficientEquity", "This organisation does not hold enough equity to transfer"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenTransferExceedsAvailableEquity_rejectsTransferFrom() {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("50");

    var hasErrors = validator.hasErrors(
        form, bindingResult, Map.of(TRANSFER_FROM, new BigDecimal("40"), TRANSFER_TO, TRANSFER_TO_EQUITY));

    assertThat(bindingResult.getFieldErrors("transferFrom"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("transferFrom.insufficientEquity", "This organisation does not hold enough equity to transfer"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenTransferEqualsAvailableEquity_returnsFalse() {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(
        form, bindingResult, Map.of(TRANSFER_FROM, new BigDecimal("40"), TRANSFER_TO, TRANSFER_TO_EQUITY));

    assertThat(hasErrors).isFalse();
  }

  @ParameterizedTest
  @MethodSource("holdingsWhereTransferToIsNotALicensee")
  void hasErrors_whenTransferToHoldsNoEquity_rejectsTransferTo(Map<Integer, BigDecimal> equityHoldings) {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(form, bindingResult, equityHoldings);

    assertThat(bindingResult.getFieldErrors("transferTo"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("transferTo.notLicensee", "This organisation is not an equity holder on this licence"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenTransferToHoldsZeroPercentBeneficialInterest_returnsFalse() {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(
        form, bindingResult, Map.of(TRANSFER_FROM, new BigDecimal("100"), TRANSFER_TO, BigDecimal.ZERO));

    assertThat(hasErrors).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"NOT_A_NUMBER", "99999999999"})
  void hasErrors_whenTransferToIsNotAnOrganisationId_rejectsTransferTo(String transferTo) {
    form.setTransferFrom(String.valueOf(TRANSFER_FROM));
    form.setTransferTo(transferTo);
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(form, bindingResult, SUFFICIENT_HOLDINGS);

    assertThat(bindingResult.getFieldErrors("transferTo"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("transferTo.notLicensee", "This organisation is not an equity holder on this licence"));
    assertThat(hasErrors).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"NOT_A_NUMBER", "99999999999"})
  void hasErrors_whenTransferFromIsNotAnOrganisationId_rejectsTransferFrom(String transferFrom) {
    form.setTransferFrom(transferFrom);
    form.setTransferTo(String.valueOf(TRANSFER_TO));
    form.getEquity().setInputValue("40");

    var hasErrors = validator.hasErrors(form, bindingResult, SUFFICIENT_HOLDINGS);

    assertThat(bindingResult.getFieldErrors("transferFrom"))
        .extracting(FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("transferFrom.insufficientEquity", "This organisation does not hold enough equity to transfer"));
    assertThat(hasErrors).isTrue();
  }

  private static Stream<Map<Integer, BigDecimal>> holdingsWhereTransferToIsNotALicensee() {
    return Stream.of(
        Map.of(TRANSFER_FROM, new BigDecimal("100"))
    );
  }
}