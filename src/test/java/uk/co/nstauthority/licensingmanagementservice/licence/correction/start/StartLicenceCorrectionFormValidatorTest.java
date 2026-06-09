package uk.co.nstauthority.licensingmanagementservice.licence.correction.start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
class StartLicenceCorrectionFormValidatorTest {

  private static final UnaryOperator<String> REQUIRED = "%s.required"::formatted;

  @InjectMocks
  private StartLicenceCorrectionFormValidator startLicenceCorrectionFormValidator;

  private StartLicenceCorrectionForm startLicenceCorrectionForm;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    startLicenceCorrectionForm = new StartLicenceCorrectionForm();
    bindingResult = new BeanPropertyBindingResult(startLicenceCorrectionForm, "form");
  }

  @Test
  void hasErrors_emptyForm() {
    var hasErrors = startLicenceCorrectionFormValidator.hasErrors(startLicenceCorrectionForm, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(
            tuple(
                "correctionReference.inputValue",
                "Enter a correction reference",
                REQUIRED.apply("correctionReference")
            ),
            tuple(
                "reason.inputValue",
                "Enter a reason",
                REQUIRED.apply("reason")
            )
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_validForm() {
    startLicenceCorrectionForm.getCorrectionReference().setInputValue("TEST-REF");
    startLicenceCorrectionForm.getReason().setInputValue("Test reason");

    var hasErrors = startLicenceCorrectionFormValidator.hasErrors(startLicenceCorrectionForm, bindingResult);

    assertThat(hasErrors).isFalse();
  }

}
