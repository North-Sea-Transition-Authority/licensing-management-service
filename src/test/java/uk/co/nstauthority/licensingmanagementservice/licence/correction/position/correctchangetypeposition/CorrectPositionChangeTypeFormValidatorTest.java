package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.correctchangetypeposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.SameTransactionPositionLookup;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder.CorrectChangeOrderService;

@ExtendWith(MockitoExtension.class)
class CorrectPositionChangeTypeFormValidatorTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.JUNE, 15);
  private static final UUID ALLOWED_POSITION_ID = UUID.randomUUID();
  private static final String ALLOWED_MOVE = ALLOWED_POSITION_ID.toString();
  private static final Set<String> ALLOWED_MOVES = Set.of(ALLOWED_MOVE);
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final UUID SOURCE_POSITION_ID = UUID.randomUUID();
  private static final UUID CHANGE_ID = UUID.randomUUID();
  private static final UUID TRANSACTION_ID = UUID.randomUUID();

  @Mock
  private LicencePositionCorrectionService licencePositionCorrectionService;

  @Mock
  private CorrectChangeOrderService correctChangeOrderService;

  private CorrectPositionChangeTypeFormValidator validator;

  private CorrectPositionChangeTypeForm form;
  private BindingResult bindingResult;

  @BeforeEach
  void setUp() {
    validator = new CorrectPositionChangeTypeFormValidator(
        Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
        licencePositionCorrectionService,
        correctChangeOrderService
    );
    form = new CorrectPositionChangeTypeForm();
    bindingResult = new BeanPropertyBindingResult(form, "form");
  }

  @Test
  void hasErrors_whenNoOptionSelected_flagsRequired() {
    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

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

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

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
    when(correctChangeOrderService.canMoveChangeToPosition(CORRECTION, SOURCE_POSITION_ID, CHANGE_ID, ALLOWED_POSITION_ID))
        .thenReturn(true);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(hasErrors).isFalse();
  }

  @Test
  void hasErrors_whenSelectedPositionAlreadyHasAPartialSurrender_flagsPartialSurrender() {
    form.getChangeTypePositionMove().setInputValue(ALLOWED_MOVE);
    when(correctChangeOrderService.canMoveChangeToPosition(CORRECTION, SOURCE_POSITION_ID, CHANGE_ID, ALLOWED_POSITION_ID))
        .thenReturn(false);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getCode, FieldError::getDefaultMessage)
        .containsExactly(tuple(
            "changeTypePositionMove.inputValue",
            "changeTypePositionMove.hasPartialSurrender",
            "Select a position that does not already have a partial surrender"
        ));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenOtherDateSelectedAndDateNotEntered_flagsIncompleteDate() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

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

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(
            tuple("correctPositionDate.dayInput.inputValue",
                "Position date must be the same as or before 15 June 2024"),
            tuple("correctPositionDate.monthInput.inputValue", ""),
            tuple("correctPositionDate.yearInput.inputValue", ""));
    assertThat(hasErrors).isTrue();
  }

  @Test
  void hasErrors_whenOtherDateSelectedAndDateIsToday_isValid() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY);
    givenNoPositionForTheTransactionOn(TODAY);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(hasErrors).isFalse();
  }

  @Test
  void hasErrors_whenOtherDateSelected_doesNotCheckAgainstAllowedMoves() {
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY.minusYears(1));
    givenNoPositionForTheTransactionOn(TODAY.minusYears(1));

    var hasErrors = validator.hasErrors(form, bindingResult, Set.of(), CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(hasErrors).isFalse();
    verifyNoInteractions(correctChangeOrderService);
  }

  @Test
  void hasErrors_whenOtherDateMovesOntoAPositionAlreadyWithAPartialSurrender_flagsPartialSurrender() {
    var matchingPositionId = UUID.randomUUID();
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY);
    when(licencePositionCorrectionService.findSameTransactionPositionOnDate(CORRECTION, SOURCE_POSITION_ID, TODAY))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, matchingPositionId));
    when(correctChangeOrderService.canMoveChangeToPosition(CORRECTION, SOURCE_POSITION_ID, CHANGE_ID, matchingPositionId))
        .thenReturn(false);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

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
  void hasErrors_whenOtherDateMovesOntoAPositionThatCanTakeTheChange_isValid() {
    var matchingPositionId = UUID.randomUUID();
    form.getChangeTypePositionMove().setInputValue(CorrectPositionChangeTypeForm.OTHER_DATE_OPTION);
    form.getCorrectPositionDate().setDate(TODAY);
    when(licencePositionCorrectionService.findSameTransactionPositionOnDate(CORRECTION, SOURCE_POSITION_ID, TODAY))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, matchingPositionId));
    when(correctChangeOrderService.canMoveChangeToPosition(CORRECTION, SOURCE_POSITION_ID, CHANGE_ID, matchingPositionId))
        .thenReturn(true);

    var hasErrors = validator.hasErrors(form, bindingResult, ALLOWED_MOVES, CORRECTION, SOURCE_POSITION_ID, CHANGE_ID);

    assertThat(hasErrors).isFalse();
  }

  private void givenNoPositionForTheTransactionOn(LocalDate positionDate) {
    when(licencePositionCorrectionService.findSameTransactionPositionOnDate(CORRECTION, SOURCE_POSITION_ID, positionDate))
        .thenReturn(new SameTransactionPositionLookup(TRANSACTION_ID, null));
  }
}