package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.recordofdecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class RecordReductionDetailsFormValidatorTest {

  @Mock
  private RecordReductionDetailsService recordReductionDetailsService;

  @Mock
  private RecordExtensionDetailsService recordExtensionDetailsService;

  @InjectMocks
  private RecordReductionDetailsFormValidator recordReductionDetailsFormValidator;

  private static final String TERM_ID = UUID.randomUUID().toString();
  private static final String PHASE_ID = UUID.randomUUID().toString();
  private static final String OTHER_TERM_ID = UUID.randomUUID().toString();
  private static final String OTHER_PHASE_ID = UUID.randomUUID().toString();

  private static final String TERM_DURATION_YEARS_FIELD = "reductionDuration[%s].years".formatted(TERM_ID);

  private ScheduleWorkProgrammeApplicationDetail applicationDetail;

  @BeforeEach
  void setUp() {
    applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.builder()
        .withId(UUID.randomUUID())
        .build();
  }

  @Test
  void isValid_whenMultipleOptionsAndNoneSelected_rejects() {
    mockViews(termView(), phaseView());
    var form = form(
        Map.of(TERM_ID, false),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(TERM_ID, null, null, null), PHASE_ID, duration(PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError("selectedTerm").getDefaultMessage())
        .isEqualTo("Select at least one phase or term being reduced");
  }

  @Test
  void isValid_whenOnlyTermOptionsAndNoneSelected_rejectsWithTermMessage() {
    mockViews(termView(), otherTermView());
    var form = form(
        Map.of(TERM_ID, false, OTHER_TERM_ID, false),
        Map.of(),
        Map.of(TERM_ID, duration(TERM_ID, null, null, null), OTHER_TERM_ID, duration(OTHER_TERM_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError("selectedTerm").getDefaultMessage())
        .isEqualTo("Select at least one term being reduced");
  }

  @Test
  void isValid_whenOnlyPhaseOptionsAndNoneSelected_rejectsWithPhaseMessage() {
    mockViews(phaseView(), otherPhaseView());
    var form = form(
        Map.of(),
        Map.of(PHASE_ID, false, OTHER_PHASE_ID, false),
        Map.of(PHASE_ID, duration(PHASE_ID, null, null, null),
            OTHER_PHASE_ID, duration(OTHER_PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError("selectedPhase").getDefaultMessage())
        .isEqualTo("Select at least one phase being reduced");
  }

  @Test
  void isValid_whenSelectedButNoDuration_rejectsDuration() {
    mockViews(termView(), phaseView());
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(TERM_ID, null, null, null), PHASE_ID, duration(PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError(TERM_DURATION_YEARS_FIELD).getDefaultMessage())
        .isEqualTo("Provide the reduction duration");
  }

  @Test
  void isValid_whenUnselectedOptionHasNoDuration_doesNotRejectIt() {
    mockViews(termView(), phaseView());
    mockTotalExtension(new ThreeFieldDuration(1, 0, 0));
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(TERM_ID, "1", "0", "0"), PHASE_ID, duration(PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isTrue();
    assertThat(errors.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenSelectedReductionsAddUpToTheExtension_hasNoErrors() {
    mockViews(termView(), otherTermView());
    mockTotalExtension(new ThreeFieldDuration(1, 0, 0));
    var form = form(
        Map.of(TERM_ID, true, OTHER_TERM_ID, true),
        Map.of(),
        Map.of(TERM_ID, duration(TERM_ID, "0", "6", "0"), OTHER_TERM_ID, duration(OTHER_TERM_ID, "0", "6", "0")));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isTrue();
  }

  @Test
  void isValid_whenTotalReductionIsLessThanTheExtension_rejectsWithTheExtensionTotal() {
    mockViews(termView(), phaseView());
    mockTotalExtension(new ThreeFieldDuration(1, 6, 0));
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(TERM_ID, "1", "0", "0"), PHASE_ID, duration(PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError(TERM_DURATION_YEARS_FIELD).getDefaultMessage())
        .isEqualTo("The total reduction must equal the total extension of 1 year 6 months");
  }

  @Test
  void isValid_whenTotalReductionIsMoreThanTheExtension_rejects() {
    mockViews(termView(), phaseView());
    mockTotalExtension(new ThreeFieldDuration(0, 0, 1));
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(TERM_ID, "0", "0", "2"), PHASE_ID, duration(PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError(TERM_DURATION_YEARS_FIELD).getDefaultMessage())
        .isEqualTo("The total reduction must equal the total extension of 1 day");
  }

  @Test
  void isValid_whenDurationIsInvalid_doesNotAlsoCheckTheTotal() {
    mockViews(termView(), phaseView());
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(TERM_ID, "abc", "0", "0"), PHASE_ID, duration(PHASE_ID, null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError(TERM_DURATION_YEARS_FIELD).getDefaultMessage())
        .isEqualTo("The duration must be a number");
  }

  @Test
  void isValid_whenSingleOption_validatesDurationAndTotalWithoutSelection() {
    mockViews(termView());
    mockTotalExtension(new ThreeFieldDuration(0, 3, 0));
    var form = form(Map.of(TERM_ID, false), Map.of(), Map.of(TERM_ID, duration(TERM_ID, "0", "3", "0")));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isTrue();
  }

  @Test
  void isValid_whenSingleOptionDoesNotMatchTheExtension_rejects() {
    mockViews(termView());
    mockTotalExtension(new ThreeFieldDuration(0, 3, 0));
    var form = form(Map.of(TERM_ID, false), Map.of(), Map.of(TERM_ID, duration(TERM_ID, "0", "4", "0")));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError(TERM_DURATION_YEARS_FIELD).getDefaultMessage())
        .isEqualTo("The total reduction must equal the total extension of 3 months");
  }

  @Test
  void isValid_whenThereAreNoOptionsToReduce_hasNoErrors() {
    mockViews();
    var form = new RecordReductionDetailsForm();
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isTrue();
  }

  @Test
  void isValid_whenSelectionMapsAreNull_initialisesThemFromViews() {
    mockViews(termView(), phaseView());
    var form = new RecordReductionDetailsForm();
    form.setSelectedTerm(null);
    form.setSelectedPhase(null);
    form.setReductionDuration(null);
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(form.getSelectedTerm()).containsEntry(TERM_ID, false);
    assertThat(form.getSelectedPhase()).containsEntry(PHASE_ID, false);
    assertThat(form.getReductionDuration()).containsKeys(TERM_ID, PHASE_ID);
  }

  @Test
  void isValid_whenDurationsAreMissing_initialisesOneForEveryOption() {
    mockViews(termView(), phaseView());
    var form = form(Map.of(TERM_ID, false), Map.of(PHASE_ID, false), Map.of());
    var errors = new BeanPropertyBindingResult(form, "form");

    recordReductionDetailsFormValidator.isValid(form, errors, applicationDetail);

    assertThat(form.getReductionDuration().get(TERM_ID).getFieldName())
        .isEqualTo("reductionDuration[%s]".formatted(TERM_ID));
    assertThat(form.getReductionDuration().get(PHASE_ID).getFieldDisplayText()).isEqualTo("reduction");
  }

  private void mockViews(RecordReductionDetailsView... views) {
    when(recordReductionDetailsService.getReductionDetailsViews(applicationDetail)).thenReturn(List.of(views));
  }

  private void mockTotalExtension(ThreeFieldDuration totalExtension) {
    when(recordExtensionDetailsService.getTotalExtensionDuration(applicationDetail)).thenReturn(totalExtension);
  }

  private RecordReductionDetailsView termView() {
    return new RecordReductionDetailsView(TERM_ID, "Second Term", "17 July 2026", false, false, null);
  }

  private RecordReductionDetailsView otherTermView() {
    return new RecordReductionDetailsView(OTHER_TERM_ID, "Third Term", "17 July 2030", false, false, null);
  }

  private RecordReductionDetailsView phaseView() {
    return new RecordReductionDetailsView(PHASE_ID, "Phase A", "17 May 2025", true, false, null);
  }

  private RecordReductionDetailsView otherPhaseView() {
    return new RecordReductionDetailsView(OTHER_PHASE_ID, "Phase B", "17 November 2025", true, false, null);
  }

  private RecordReductionDetailsForm form(
      Map<String, Boolean> selectedTerm,
      Map<String, Boolean> selectedPhase,
      Map<String, ThreeFieldDurationInput> reductionDuration
  ) {
    var form = new RecordReductionDetailsForm();
    form.setSelectedTerm(new HashMap<>(selectedTerm));
    form.setSelectedPhase(new HashMap<>(selectedPhase));
    form.setReductionDuration(new HashMap<>(reductionDuration));
    return form;
  }

  private ThreeFieldDurationInput duration(String id, String years, String months, String days) {
    var input = RecordReductionDetailsForm.newDurationInput(id);
    input.setYears(years);
    input.setMonths(months);
    input.setDays(days);
    return input;
  }
}
