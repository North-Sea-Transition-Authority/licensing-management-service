package uk.co.nstauthority.licensingmanagementservice.licence.continuation.requirementjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class LicenceContinuationLicenceOperatorsValidatorTest {

  private LicenceContinuationLicenceOperatorsValidator validator;
  @BeforeEach
  void setUp() {
    validator = new LicenceContinuationLicenceOperatorsValidator();
  }

  @Test
  void isValid_whenHasMissingOperatorsIsTrueAndExplanationIsFilled_isValid() {
    var form = new LicenceContinuationLicenceOperatorsForm();
    form.setPendingActionsExplanation("We are in the process of tendering an operator.");
    Errors errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors, true);

    assertThat(result).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenHasMissingOperatorsIsFalseAndExplanationIsEmpty_isValid() {
    var form = new LicenceContinuationLicenceOperatorsForm();
    form.setPendingActionsExplanation("");
    Errors errors = new BeanPropertyBindingResult(form, "form");

    boolean result = validator.isValid(errors, false);

    assertThat(result).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }
}