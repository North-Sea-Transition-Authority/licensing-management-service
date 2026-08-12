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
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class RecordExtensionDetailsFormValidatorTest {

  @Mock
  private RecordExtensionDetailsService recordExtensionDetailsService;

  @InjectMocks
  private RecordExtensionDetailsFormValidator recordExtensionDetailsFormValidator;

  private static final String TERM_ID = UUID.randomUUID().toString();
  private static final String PHASE_ID = UUID.randomUUID().toString();
  private static final String OTHER_TERM_ID = UUID.randomUUID().toString();
  private static final String OTHER_PHASE_ID = UUID.randomUUID().toString();

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
        Map.of(TERM_ID, duration(null, null, null), PHASE_ID, duration(null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError("selectedTerm").getDefaultMessage())
        .isEqualTo("Select at least one phase or term being extended");
  }

  @Test
  void isValid_whenSelectedButNoDuration_rejectsDuration() {
    mockViews(termView(), phaseView());
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration(null, null, null), PHASE_ID, duration(null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.hasFieldErrors("extensionDuration[" + TERM_ID + "].days")).isTrue();
  }

  @Test
  void isValid_whenSelectedWithValidDuration_hasNoErrors() {
    mockViews(termView(), phaseView());
    var form = form(
        Map.of(TERM_ID, true),
        Map.of(PHASE_ID, false),
        Map.of(TERM_ID, duration("1", "0", "0"), PHASE_ID, duration(null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isTrue();
  }

  @Test
  void isValid_whenSingleOption_validatesDurationWithoutSelection() {
    mockViews(termView());
    var form = form(Map.of(), Map.of(), Map.of(TERM_ID, duration("1", "0", "0")));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isTrue();
  }

  @Test
  void isValid_whenOnlyTermOptionsAndNoneSelected_rejectsWithTermMessage() {
    mockViews(termView(), otherTermView());
    var form = form(
        Map.of(TERM_ID, false, OTHER_TERM_ID, false),
        Map.of(),
        Map.of(TERM_ID, duration(null, null, null), OTHER_TERM_ID, duration(null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError("selectedTerm").getDefaultMessage())
        .isEqualTo("Select at least one term being extended");
  }

  @Test
  void isValid_whenOnlyPhaseOptionsAndNoneSelected_rejectsWithPhaseMessage() {
    mockViews(phaseView(), otherPhaseView());
    var form = form(
        Map.of(),
        Map.of(PHASE_ID, false, OTHER_PHASE_ID, false),
        Map.of(PHASE_ID, duration(null, null, null), OTHER_PHASE_ID, duration(null, null, null)));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.getFieldError("selectedPhase").getDefaultMessage())
        .isEqualTo("Select at least one phase being extended");
  }

  @Test
  void isValid_whenSelectionMapsAreNull_initialisesThemFromViews() {
    mockViews(termView(), phaseView());
    var form = new RecordExtensionDetailsForm();
    form.setSelectedTerm(null);
    form.setSelectedPhase(null);
    form.setExtensionDuration(new HashMap<>(
        Map.of(TERM_ID, duration(null, null, null), PHASE_ID, duration(null, null, null))));
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(form.getSelectedTerm()).containsEntry(TERM_ID, false);
    assertThat(form.getSelectedPhase()).containsEntry(PHASE_ID, false);
  }

  @Test
  void isValid_whenSelectedDurationIsNull_rejectsDurationRatherThanFailingOnSave() {
    mockViews(termView(), phaseView());
    var durations = new HashMap<String, ThreeFieldDurationInput>();
    durations.put(TERM_ID, null);
    durations.put(PHASE_ID, duration(null, null, null));
    var form = form(Map.of(TERM_ID, true), Map.of(PHASE_ID, false), durations);
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.hasFieldErrors("extensionDuration[" + TERM_ID + "].days")).isTrue();
  }

  @Test
  void isValid_whenSelectedDurationWasNotSubmittedAtAll_rejectsDuration() {
    mockViews(termView(), phaseView());
    var durations = new HashMap<String, ThreeFieldDurationInput>();
    durations.put(PHASE_ID, duration(null, null, null));
    var form = form(Map.of(TERM_ID, true), Map.of(PHASE_ID, false), durations);
    var errors = new BeanPropertyBindingResult(form, "form");

    assertThat(recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail)).isFalse();
    assertThat(errors.hasFieldErrors("extensionDuration[" + TERM_ID + "].days")).isTrue();
  }

  @Test
  void isValid_whenDurationsAreMissing_initialisesOneForEveryOption() {
    mockViews(termView(), phaseView());
    var form = form(Map.of(TERM_ID, false), Map.of(PHASE_ID, false), Map.of());
    var errors = new BeanPropertyBindingResult(form, "form");

    recordExtensionDetailsFormValidator.isValid(form, errors, applicationDetail);

    assertThat(form.getExtensionDuration()).containsOnlyKeys(TERM_ID, PHASE_ID);
  }

  private void mockViews(RecordExtensionDetailsView... views) {
    when(recordExtensionDetailsService.getExtensionDetailsViews(applicationDetail)).thenReturn(List.of(views));
  }

  private RecordExtensionDetailsView termView() {
    return new RecordExtensionDetailsView(TERM_ID, "Second term", "17 July 2026", false, false, null);
  }

  private RecordExtensionDetailsView phaseView() {
    return new RecordExtensionDetailsView(PHASE_ID, "Phase A", "17 May 2025", true, false, null);
  }

  private RecordExtensionDetailsView otherTermView() {
    return new RecordExtensionDetailsView(OTHER_TERM_ID, "Third term", "17 July 2027", false, false, null);
  }

  private RecordExtensionDetailsView otherPhaseView() {
    return new RecordExtensionDetailsView(OTHER_PHASE_ID, "Phase B", "17 May 2026", true, false, null);
  }

  private RecordExtensionDetailsForm form(
      Map<String, Boolean> selectedTerm,
      Map<String, Boolean> selectedPhase,
      Map<String, ThreeFieldDurationInput> extensionDuration) {
    var form = new RecordExtensionDetailsForm();
    form.setSelectedTerm(new HashMap<>(selectedTerm));
    form.setSelectedPhase(new HashMap<>(selectedPhase));
    form.setExtensionDuration(new HashMap<>(extensionDuration));
    return form;
  }

  private ThreeFieldDurationInput duration(String years, String months, String days) {
    var input = new ThreeFieldDurationInput("extensionDuration[" + TERM_ID + "]", "extension");
    input.setYears(years);
    input.setMonths(months);
    input.setDays(days);
    return input;
  }
}
