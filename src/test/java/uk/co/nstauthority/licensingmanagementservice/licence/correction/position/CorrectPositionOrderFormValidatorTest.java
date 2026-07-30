package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

class CorrectPositionOrderFormValidatorTest {

  private static final String ALLOWED_MOVE =
      new PositionMove(PositionMoveDirection.AFTER, UUID.randomUUID()).toFormValue();
  private static final Set<String> ALLOWED_MOVES = Set.of(ALLOWED_MOVE);

  private final CorrectPositionOrderFormValidator validator = new CorrectPositionOrderFormValidator();

  private CorrectPositionOrderForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    form = new CorrectPositionOrderForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoOptionSelected_flagsRequired() {
    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode)
        .containsExactly(tuple("positionMove.inputValue", "positionMove.required"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenSelectedOptionNotAllowed_flagsInvalid() {
    form.getPositionMove().setInputValue(
        new PositionMove(PositionMoveDirection.BEFORE, UUID.randomUUID()).toFormValue());

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "positionMove.inputValue", "positionMove.invalid", "Select where to move the position to"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenSelectedOptionIsAllowed_isValid() {
    form.getPositionMove().setInputValue(ALLOWED_MOVE);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(hasErrors).isFalse();
  }
}