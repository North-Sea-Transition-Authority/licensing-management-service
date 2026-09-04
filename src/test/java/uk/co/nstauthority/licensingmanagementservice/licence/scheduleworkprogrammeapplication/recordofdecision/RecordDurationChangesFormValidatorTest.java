package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class RecordDurationChangesFormValidatorTest {

  private static final String INITIAL_ID = UUID.randomUUID().toString();
  private static final String SECOND_ID = UUID.randomUUID().toString();
  private static final String THIRD_ID = UUID.randomUUID().toString();

  @Mock
  private RecordDurationChangesService recordDurationChangesService;

  @InjectMocks
  private RecordDurationChangesFormValidator recordDurationChangesFormValidator;

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void isValid_whenAnExtensionIsBalancedByAReduction_assertNoErrors() {
    var form = formWith(
        change(INITIAL_ID, DurationChangeType.EXTEND, 1, 0, 0),
        change(SECOND_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(THIRD_ID, DurationChangeType.REDUCE, 1, 0, 0));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    mockViews();

    var isValid = recordDurationChangesFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenTheTotalsDoNotBalance_assertError() {
    var form = formWith(
        change(INITIAL_ID, DurationChangeType.EXTEND, 2, 0, 0),
        change(SECOND_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(THIRD_ID, DurationChangeType.REDUCE, 1, 0, 0));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    mockViews();

    var isValid = recordDurationChangesFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getDefaultMessage)
        .containsExactly(RecordDurationChangesFormValidator.TOTAL_MISMATCH_ERROR_MESSAGE
            .formatted("1 year", "2 years"));
  }

  @Test
  void isValid_whenAPeriodHasNoAnswer_assertError() {
    var form = formWith(
        change(INITIAL_ID, null, 0, 0, 0),
        change(SECOND_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(THIRD_ID, DurationChangeType.MAINTAIN, 0, 0, 0));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    mockViews();

    var isValid = recordDurationChangesFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getDefaultMessage)
        .containsExactly(RecordDurationChangesFormValidator.CHANGE_TYPE_REQUIRED_ERROR_MESSAGE
            .formatted(TermType.INITIAL.getDisplayName()));
  }

  @Test
  void isValid_whenTheCurrentPeriodIsReduced_assertError() {
    var form = formWith(
        change(INITIAL_ID, DurationChangeType.REDUCE, 1, 0, 0),
        change(SECOND_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(THIRD_ID, DurationChangeType.MAINTAIN, 0, 0, 0));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    mockViews();

    var isValid = recordDurationChangesFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getDefaultMessage)
        .containsExactly(RecordDurationChangesFormValidator.REDUCE_NOT_AVAILABLE_ERROR_MESSAGE
            .formatted(TermType.INITIAL.getDisplayName()));
  }

  @Test
  void isValid_whenTheFinalPeriodIsExtended_assertError() {
    var form = formWith(
        change(INITIAL_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(SECOND_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(THIRD_ID, DurationChangeType.EXTEND, 1, 0, 0));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    mockViews();

    var isValid = recordDurationChangesFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getDefaultMessage)
        .containsExactly(RecordDurationChangesFormValidator.EXTEND_NOT_AVAILABLE_ERROR_MESSAGE
            .formatted(TermType.THIRD.getDisplayName()));
  }

  @Test
  void isValid_whenEverythingIsMaintained_assertNoErrors() {
    var form = formWith(
        change(INITIAL_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(SECOND_ID, DurationChangeType.MAINTAIN, 0, 0, 0),
        change(THIRD_ID, DurationChangeType.MAINTAIN, 0, 0, 0));
    var bindingResult = new BeanPropertyBindingResult(form, "form");
    mockViews();

    var isValid = recordDurationChangesFormValidator.isValid(form, bindingResult, applicationDetail);

    assertThat(isValid).isTrue();
  }

  private record Change(String id, DurationChangeType changeType, int years, int months, int days) {
  }

  private Change change(String id, DurationChangeType changeType, int years, int months, int days) {
    return new Change(id, changeType, years, months, days);
  }

  private RecordDurationChangesForm formWith(Change... changes) {
    var form = new RecordDurationChangesForm();

    for (var change : changes) {
      form.getChangeType().put(change.id(), change.changeType());

      var reduceInput = RecordDurationChangesForm.newReduceDurationInput(change.id());
      var extendInput = RecordDurationChangesForm.newExtendDurationInput(change.id());
      for (var input : List.of(reduceInput, extendInput)) {
        input.setYears(String.valueOf(change.years()));
        input.setMonths(String.valueOf(change.months()));
        input.setDays(String.valueOf(change.days()));
      }

      form.getReduceDuration().put(change.id(), reduceInput);
      form.getExtendDuration().put(change.id(), extendInput);
    }

    return form;
  }

  private void mockViews() {
    when(recordDurationChangesService.getDurationChangeViews(applicationDetail))
        .thenReturn(List.of(
            view(INITIAL_ID, TermType.INITIAL, false, true),
            view(SECOND_ID, TermType.SECOND, true, true),
            view(THIRD_ID, TermType.THIRD, true, false)));
  }

  private RecordDurationChangeView view(String id, TermType termType, boolean canReduce, boolean canExtend) {
    return new RecordDurationChangeView(
        id,
        termType.getDisplayName(),
        false,
        "31 December 2027",
        "4 years",
        canReduce,
        canExtend);
  }
}
