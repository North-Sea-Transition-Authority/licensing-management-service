package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

class LicencePositionFeatureTestHarnessFormValidatorTest {

  private LicencePositionFeatureTestHarnessForm form;
  private BindingResult bindingResult;
  private final LicencePositionFeatureTestHarnessFormValidator licencePositionFeatureTestHarnessFormValidator =
      new LicencePositionFeatureTestHarnessFormValidator();

  @BeforeEach
  void setUp() {
    form = new LicencePositionFeatureTestHarnessForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoLicenceSelected() {
    var hasErrors = licencePositionFeatureTestHarnessFormValidator.hasErrors(form, bindingResult, null);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(tuple("licenceId.inputValue", "Select a licence", "licenceId.required"));

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenLicenceHasNoPositions() {
    form.getLicenceId().setInputValue("1");

    var hasErrors = licencePositionFeatureTestHarnessFormValidator.hasErrors(
        form, bindingResult, new LicencePositionFeatureSeedState(0, false));

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(tuple(
            "licenceId.inputValue",
            "Select a licence that has positions to link features to",
            "licenceId.noPositions"));

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenLicencePositionsAlreadyHoldFeatures() {
    form.getLicenceId().setInputValue("1");

    var hasErrors = licencePositionFeatureTestHarnessFormValidator.hasErrors(
        form, bindingResult, new LicencePositionFeatureSeedState(3, true));

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(tuple(
            "licenceId.inputValue",
            "Select a licence that has no features linked to its positions yet",
            "licenceId.hasLinkedFeatures"));

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenLicenceHasUnseededPositions() {
    form.getLicenceId().setInputValue("1");

    var hasErrors = licencePositionFeatureTestHarnessFormValidator.hasErrors(
        form, bindingResult, new LicencePositionFeatureSeedState(3, false));

    assertThat(bindingResult.getFieldErrors()).isEmpty();
    assertThat(hasErrors).isFalse();
  }
}
