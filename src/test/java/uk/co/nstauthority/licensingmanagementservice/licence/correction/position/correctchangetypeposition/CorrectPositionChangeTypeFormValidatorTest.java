package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

class CorrectPositionChangeTypeFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);
  private static final String ALLOWED_MOVE = UUID.randomUUID().toString();
  private static final List<String> ALLOWED_MOVES = List.of(ALLOWED_MOVE);

  private final CorrectPositionChangeTypeFormValidator validator = new CorrectPositionChangeTypeFormValidator(
      Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));

  private CorrectPositionChangeTypeForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    form = new CorrectPositionChangeTypeForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoOptionSelected_flagsRequired() {
    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "changeTypePositionMove.inputValue",
            "changeTypePositionMove.required",
            "Select where to move the change to"
        ));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenSelectedPositionIsNotAllowed_flagsInvalid() {
    form.getChangeTypePositionMove().setInputValue(UUID.randomUUID().toString());

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "changeTypePositionMove.inputValue",
            "changeTypePositionMove.invalid",
            "Select where to move the change to"
        ));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenSelectedPositionIsAllowed_isValid() {
    form.getChangeTypePositionMove().setInputValue(ALLOWED_MOVE);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(hasErrors).isFalse();
  }

  @Test
  void hasErrors_whenOtherDateSelectedAndDateNotEntered_flagsIncompleteDate() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue", "Enter a complete position date"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", ""));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenOtherDateSelectedAndDateInFuture_flagsDateTooLate() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY.plusDays(1));

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue",
                "Position date must be the same as or before 15 Jun 2024"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", ""));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenOtherDateSelectedAndDateIsToday_isValid() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES);

    assertThat(hasErrors).isFalse();
  }

  @Test
  void hasErrors_whenOtherDateSelected_doesNotCheckAgainstAllowedMoves() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY.minusYears(1));

    var hasErrors = validator.hasErrors(form, bindingResult, List.of());

    assertThat(hasErrors).isFalse();
  }
}