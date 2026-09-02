package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import java.util.Map;
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
  private static final Map<String, String> ALLOCATABLE_USERS = Map.of("1", "Jane Doe");

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
        .hasErrors(updateCorrectionGeneralDetailsForm, bindingResult, ALLOCATABLE_USERS);

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
            ),
            tuple(
                "allocatedToWuaId",
                "Select the user to allocate this correction to",
                REQUIRED.apply("allocatedToWuaId")
            )
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenAllocatedUserIsNotAnAllocatableUser() {
    updateCorrectionGeneralDetailsForm.getCorrectionReference().setInputValue("TEST-REF");
    updateCorrectionGeneralDetailsForm.getReason().setInputValue("Test reason");
    updateCorrectionGeneralDetailsForm.setAllocatedToWuaId("999");

    var hasErrors = updateCorrectionGeneralDetailsFormValidator
        .hasErrors(updateCorrectionGeneralDetailsForm, bindingResult, ALLOCATABLE_USERS);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage, FieldError::getCode)
        .containsExactly(
            tuple(
                "allocatedToWuaId",
                "Select a valid user to allocate this correction to",
                "allocatedToWuaId.invalid"
            )
        );

    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_validForm() {
    updateCorrectionGeneralDetailsForm.getCorrectionReference().setInputValue("TEST-REF");
    updateCorrectionGeneralDetailsForm.getReason().setInputValue("Test reason");
    updateCorrectionGeneralDetailsForm.setAllocatedToWuaId("1");

    var hasErrors = updateCorrectionGeneralDetailsFormValidator
        .hasErrors(updateCorrectionGeneralDetailsForm, bindingResult, ALLOCATABLE_USERS);

    assertThat(hasErrors).isFalse();
  }
}