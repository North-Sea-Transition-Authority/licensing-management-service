package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    when(licenceScheduleExtensionFormService.getNewLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();

    durationMap.put("test", createValidDurationInput("extensionDuration[test]"));
    form.setExtensionDuration(durationMap);

    form.setSelectedPhase(Map.of("test", true));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(
        form, bindingResult,
        scheduleWorkProgrammeApplicationDetail
    )).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_whenSingleTermIsSelected() {
    when(licenceScheduleExtensionFormService.getNewLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();

    durationMap.put("test", createValidDurationInput("extensionDuration[test]"));
    form.setExtensionDuration(durationMap);

    form.setSelectedTerm(Map.of("test", true));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(
        form, bindingResult,
        scheduleWorkProgrammeApplicationDetail
    )).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void InValid_whenMultipleDurationsButNoSelection() {
    when(licenceScheduleExtensionFormService.getExtendableTermAndPhases(
        any())).thenReturn(List.of(new LicenceScheduleTermAndPhases("1", "Term A", Collections.emptyList())));

    when(licenceScheduleExtensionFormService.getNewLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());

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
    when(licenceScheduleExtensionFormService.getNewLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());

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
    when(licenceScheduleExtensionFormService.getNewLicenceScheduleExtensionForm(any())).thenReturn(new LicenceScheduleExtensionForm());

    var form = new LicenceScheduleExtensionForm();
    Map<String, ThreeFieldDurationInput> durationMap = new HashMap<>();
    String key = "test";

    durationMap.put(key, createValidDurationInput("extensionDuration[" + key + "]"));
    form.setExtensionDuration(durationMap);

    form.setSelectedPhase(Map.of(key, true));
    form.setSelectedTerm(Map.of(key, true));

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
}