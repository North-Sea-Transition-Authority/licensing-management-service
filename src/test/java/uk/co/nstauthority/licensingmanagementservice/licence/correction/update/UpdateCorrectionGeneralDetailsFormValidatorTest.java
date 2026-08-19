package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

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
class UpdateCorrectionGeneralDetailsFormValidatorTest {

  private static final UnaryOperator<String> REQUIRED = "%s.required"::formatted;

  @InjectMocks
  private UpdateCorrectionGeneralDetailsFormValidator updateCorrectionGeneralDetailsFormValidator;

  private UpdateCorrectionGeneralDetailsForm updateCorrectionGeneralDetailsForm;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    updateCorrectionGeneralDetailsForm = new UpdateCorrectionGeneralDetailsForm();
    bindingResult = new BeanPropertyBindingResult(updateCorrectionGeneralDetailsForm, "form");
  }

  @Test
  void hasErrors_emptyForm() {
    var hasErrors = updateCorrectionGeneralDetailsFormValidator
        .hasErrors(updateCorrectionGeneralDetailsForm, bindingResult);

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
    updateCorrectionGeneralDetailsForm.getCorrectionReference().setInputValue("TEST-REF");
    updateCorrectionGeneralDetailsForm.getReason().setInputValue("Test reason");

    var hasErrors = updateCorrectionGeneralDetailsFormValidator
        .hasErrors(updateCorrectionGeneralDetailsForm, bindingResult);

    assertThat(hasErrors).isFalse();
  }
}