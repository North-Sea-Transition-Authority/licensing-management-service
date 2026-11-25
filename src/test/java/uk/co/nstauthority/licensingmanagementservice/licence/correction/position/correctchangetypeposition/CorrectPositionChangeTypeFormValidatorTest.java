package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.SameTransactionPositionLookup;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;

@ExtendWith(MockitoExtension.class)
class CorrectPositionChangeTypeFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final UUID SOURCE_POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();
  private static final UUID TRANSACTION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  @Mock
  private CorrectChangeOrderService correctChangeOrderService;

  private CorrectPositionChangeTypeFormValidator validator;

  private CorrectPositionChangeTypeForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    validator = new CorrectPositionChangeTypeFormValidator(
        Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
        licencePositionStagedPayloadService,
        correctChangeOrderService
    );
    form = new CorrectPositionChangeTypeForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenDateNotEntered_flagsIncompleteDate() {
    var hasErrors = validator.hasErrors(form, bindingResult, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue", "Enter a complete execution date"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", ""));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenDateInFuture_flagsDateTooLate() {
    form.getCorrectPositionDate().setDate(TODAY.plusDays(1));

    var hasErrors = validator.hasErrors(form, bindingResult, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue",
                "Execution date must be the same as or before 15 June 2024"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", ""));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenDateIsTodayAndNoPositionForTheTransaction_isValid() {
    form.getCorrectPositionDate().setDate(TODAY);
    when(licencePositionStagedPayloadService.findSameTransactionPositionOnDate(CORRECTION, SOURCE_POSITION_ID, TODAY))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, null));

    var hasErrors = validator.hasErrors(form, bindingResult, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(hasErrors).isFalse();
  }

  @Test
  void hasErrors_whenDateMovesOntoAPositionAlreadyWithAPartialSurrender_flagsPartialSurrender() {
    var matchingPositionId = UUID.randomUUID();
    form.getCorrectPositionDate().setDate(TODAY);
    when(licencePositionStagedPayloadService.findSameTransactionPositionOnDate(CORRECTION, SOURCE_POSITION_ID, TODAY))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, matchingPositionId));
    when(correctChangeOrderService.canMoveChangeToPosition(CORRECTION, SOURCE_POSITION_ID, CHANGE_ID, matchingPositionId))
        .thenReturn(false);

    var hasErrors = validator.hasErrors(form, bindingResult, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple(
                "correctPositionDate.dayInput.inputValue",
                "Enter a date that does not already have a partial surrender for this transaction"
            ),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", "")
        );
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenDateMovesOntoAPositionThatCanTakeTheChange_isValid() {
    var matchingPositionId = UUID.randomUUID();
    form.getCorrectPositionDate().setDate(TODAY);
    when(licencePositionStagedPayloadService.findSameTransactionPositionOnDate(CORRECTION, SOURCE_POSITION_ID, TODAY))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, matchingPositionId));
    when(correctChangeOrderService.canMoveChangeToPosition(CORRECTION, SOURCE_POSITION_ID, CHANGE_ID, matchingPositionId))
        .thenReturn(true);

    var hasErrors = validator.hasErrors(form, bindingResult, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(hasErrors).isFalse();
  }
}
