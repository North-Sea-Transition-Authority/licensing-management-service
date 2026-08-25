package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMove;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.PositionMoveDirection;

class CorrectChangeOrderFormValidatorTest {

  private static final String ALLOWED_MOVE =
      new PositionMove(PositionMoveDirection.AFTER, UUID.randomUUID()).toFormValue();
  private static final Set<String> ALLOWED_MOVES = Set.of(ALLOWED_MOVE);

  private final CorrectChangeOrderFormValidator validator = new CorrectChangeOrderFormValidator();

  private CorrectChangeOrderForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    form = new CorrectChangeOrderForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoOptionSelected_flagsRequired() {
    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode)
        .containsExactly(tuple("changeMove.inputValue", "changeMove.required"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenSelectedOptionNotAllowed_flagsInvalid() {
    form.getChangeMove().setInputValue(
        new PositionMove(PositionMoveDirection.BEFORE, UUID.randomUUID()).toFormValue());

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "changeMove.inputValue", "changeMove.invalid", "Select where to move the change to"));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenSelectedOptionIsAllowed_isValid() {
    form.getChangeMove().setInputValue(ALLOWED_MOVE);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(hasErrors).isFalse();
  }
}