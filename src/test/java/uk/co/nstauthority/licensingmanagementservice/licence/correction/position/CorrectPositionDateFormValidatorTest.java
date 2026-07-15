package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
class CorrectPositionDateFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);

  @Mock
  private Clock clock;

  @InjectMocks
  private CorrectPositionDateFormValidator correctPositionDateFormValidator;

  private CorrectPositionDateForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);

    form = new CorrectPositionDateForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_emptyForm() {
    var hasErrors = correctPositionDateFormValidator.hasErrors(form, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue", "Enter a complete a position date"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", "")
        );
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_dateInFuture() {
    form.getCorrectPositionDate().setDate(TODAY.plusDays(1));

    var hasErrors = correctPositionDateFormValidator.hasErrors(form, bindingResult);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue", "A position date must be the same as or before 15 Jun 2024"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", "")
        );
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_today_isValid() {
    form.getCorrectPositionDate().setDate(TODAY);

    var hasErrors = correctPositionDateFormValidator.hasErrors(form, bindingResult);

    assertThat(hasErrors).isFalse();
  }

  @Test
  void hasErrors_pastDate_isValid() {
    form.getCorrectPositionDate().setDate(TODAY.minusYears(1));

    var hasErrors = correctPositionDateFormValidator.hasErrors(form, bindingResult);

    assertThat(hasErrors).isFalse();
  }
}