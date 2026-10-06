package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.validation.FieldError;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionFormValidatorTest {

  @InjectMocks
  private LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionFormService;

  @BeforeEach
  void setUp() {
    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
  }

  @Test
  void isValid_whenSinglePhaseIsSelected() {
    LicenceScheduleExtensionRequestView licenceScheduleExtensionRequestView = createMockView("test", true);
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(any()))
        .thenReturn(List.of(licenceScheduleExtensionRequestView));

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();

    durationMap.put("test", createValidDurationInput("extensionDuration[test]"));
    form.setExtensionDuration(durationMap);

    form.setSelectedPhase(new HashMap<>(Map.of("test", true)));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(
        form, bindingResult,
        scheduleWorkProgrammeApplicationDetail
    )).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenSingleTermIsSelected() {
    LicenceScheduleExtensionRequestView licenceScheduleExtensionRequestView = createMockView("test", false);
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(any()))
        .thenReturn(List.of(licenceScheduleExtensionRequestView));

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();

    durationMap.put("test", createValidDurationInput("extensionDuration[test]"));
    form.setExtensionDuration(durationMap);

    form.setSelectedTerm(new HashMap<>(Map.of("test", true)));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(
        form, bindingResult,
        scheduleWorkProgrammeApplicationDetail
    )).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void InValid_whenMultipleDurationsButNoSelection() {
    LicenceScheduleExtensionRequestView termView1 = createMockView("test", false);
    LicenceScheduleExtensionRequestView termView2 = createMockView("test1", false);

    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(any()))
        .thenReturn(List.of(termView1, termView2));

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();

    durationMap.put("test", createValidDurationInput("extensionDuration[test]"));
    durationMap.put("test1", createValidDurationInput("extensionDuration[test1]"));

    form.setExtensionDuration(durationMap);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail)).isFalse();
  }

  @Test
  void InValid_whenMultipleDurationsNotFilled() {
    LicenceScheduleExtensionRequestView licenceScheduleExtensionRequestView = createMockView("test", true);
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(any()))
        .thenReturn(List.of(licenceScheduleExtensionRequestView));

    var form = new LicenceScheduleExtensionForm();
    String key = "test";

    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();
    ThreeFieldDurationInput phaseDuration = new ThreeFieldDurationInput("extensionDuration[" + key + "]", "extension");
    durationMap.put(key, phaseDuration);
    form.setExtensionDuration(durationMap);

    form.setSelectedPhase(new HashMap<>(Map.of(key, true)));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(
        form, bindingResult,
        scheduleWorkProgrammeApplicationDetail
    )).isFalse();

    String expectedFieldErrorPath = "extensionDuration[" + key + "].years";
    assertThat(bindingResult.hasFieldErrors(expectedFieldErrorPath)).isTrue();
  }

  @Test
  void isValid_whenMultipleItemsAreSelected_shouldBeTrue() {
    LicenceScheduleExtensionRequestView licenceScheduleExtensionRequestView = createMockView("test", true);
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(any()))
        .thenReturn(List.of(licenceScheduleExtensionRequestView));

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();
    String key = "test";

    durationMap.put(key, createValidDurationInput("extensionDuration[" + key + "]"));
    form.setExtensionDuration(durationMap);

    form.setSelectedPhase(new HashMap<>(Map.of(key, true)));
    form.setSelectedTerm(new HashMap<>(Map.of(key, true)));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(
        form, bindingResult,
        scheduleWorkProgrammeApplicationDetail
    )).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  private ThreeFieldDurationInput createValidDurationInput(String inputName) {
    ThreeFieldDurationInput durationInput = new ThreeFieldDurationInput(inputName, "label");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");
    return durationInput;
  }

  @Test
  void isValid_whenAStaleFormSelectsATermThatCanNoLongerBeExtended_assertItIsRemovedFromTheForm() {
    var endedTermId = UUID.randomUUID().toString();
    var liveTermId = UUID.randomUUID().toString();
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(new LicenceScheduleExtensionRequestView(liveTermId, "Second term", false, false, null)));

    var form = new LicenceScheduleExtensionForm();
    form.setSelectedTerm(new HashMap<>(Map.of(endedTermId, true, liveTermId, true)));
    form.setExtensionDuration(new HashMap<>(Map.of(
        endedTermId, createValidDurationInput("extensionDuration[%s]".formatted(endedTermId)),
        liveTermId, createValidDurationInput("extensionDuration[%s]".formatted(liveTermId))
    )));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = licenceScheduleExtensionFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail);

    assertThat(isValid).isTrue();
    assertThat(form.getSelectedTerm()).containsOnlyKeys(liveTermId);
    assertThat(form.getExtensionDuration()).containsOnlyKeys(liveTermId);
  }

  @Test
  void isValid_whenAStaleFormOnlySelectsAnEndedTerm_assertSelectionRequiredRatherThanSavingALiveTerm() {
    var endedTermId = UUID.randomUUID().toString();
    var secondTermId = UUID.randomUUID().toString();
    var thirdTermId = UUID.randomUUID().toString();
    when(licenceScheduleExtensionFormService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(
            new LicenceScheduleExtensionRequestView(secondTermId, "Second Term", false, false, null),
            new LicenceScheduleExtensionRequestView(thirdTermId, "Third Term", false, false, null)));

    var form = new LicenceScheduleExtensionForm();
    form.setSelectedTerm(new HashMap<>(Map.of(endedTermId, true, secondTermId, false)));
    form.setExtensionDuration(new HashMap<>(Map.of(
        endedTermId, createValidDurationInput("extensionDuration[%s]".formatted(endedTermId)),
        secondTermId, createValidDurationInput("extensionDuration[%s]".formatted(secondTermId))
    )));
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    var isValid = licenceScheduleExtensionFormValidator.isValid(form, bindingResult, scheduleWorkProgrammeApplicationDetail);

    assertThat(isValid).isFalse();
    assertThat(bindingResult.getFieldErrors())
        .extracting(FieldError::getField, FieldError::getDefaultMessage)
        .containsExactly(tuple("selectedTerm", "Select at least one term to request extension"));
  }

  private LicenceScheduleExtensionRequestView createMockView(String id, boolean isPhase) {
    LicenceScheduleExtensionRequestView licenceScheduleExtensionRequestView = mock(LicenceScheduleExtensionRequestView.class);
    when(licenceScheduleExtensionRequestView.id()).thenReturn(id);
    when(licenceScheduleExtensionRequestView.isPhase()).thenReturn(isPhase);
    return licenceScheduleExtensionRequestView;
  }
}