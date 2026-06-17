package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
class LicencePositionTestHarnessFormValidatorTest {

  private LicencePositionTestHarnessForm licencePositionTestHarnessForm;
  private BindingResult bindingResult;
  private final LicencePositionTestHarnessFormValidator licencePositionTestHarnessFormValidator =  new LicencePositionTestHarnessFormValidator();

  @BeforeEach
  void setUp() {
    licencePositionTestHarnessForm = new LicencePositionTestHarnessForm();
    bindingResult = new BeanPropertyBindingResult(licencePositionTestHarnessForm, "form");
  }

  @Test
  void hasErrors_emptyForm(){
    var hasErrors = licencePositionTestHarnessFormValidator.hasErrors(licencePositionTestHarnessForm, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(
            tuple(
                "licenceId.inputValue",
                "Select a licence",
                "licenceId.required"
            ),
            tuple(
                "secondaryLicenceId.inputValue",
                "Select a second licence",
                "secondaryLicenceId.required"
            )
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_duplicateLicences(){
    licencePositionTestHarnessForm.getLicenceId().setInputValue("1");
    licencePositionTestHarnessForm.getSecondaryLicenceId().setInputValue("1");

    var hasErrors = licencePositionTestHarnessFormValidator.hasErrors(licencePositionTestHarnessForm, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(
            tuple(
                "secondaryLicenceId.inputValue",
                "Select a second licence that differs from the first",
                "secondaryLicenceId.duplicate"
            )
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_validForm() {
    licencePositionTestHarnessForm.getLicenceId().setInputValue("1");
    licencePositionTestHarnessForm.getSecondaryLicenceId().setInputValue("2");

    var hasErrors = licencePositionTestHarnessFormValidator.hasErrors(licencePositionTestHarnessForm, bindingResult);

    assertThat(hasErrors).isFalse();
  }
}