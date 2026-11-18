package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.ScheduleRelativeDateValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class OtherScheduleEventFormValidatorTest {

  @Mock
  private ScheduleRelativeDateValidationService scheduleRelativeDateValidationService;

  @InjectMocks
  private OtherScheduleEventFormValidator validator;

  private LicenceScheduleDetail licenceScheduleDetail = new LicenceScheduleDetail();

  @Test
  void isValid_relativeDate() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.setRelativeEventId(UUID.randomUUID().toString());
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1,0,0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
    verify(scheduleRelativeDateValidationService).validateRelativeDateBeforeEndOfSchedule(
        licenceScheduleDetail, form.getRelativeDuration(), UUID.fromString(form.getRelativeEventId()), bindingResult);
  }

  @Test
  void isValid_withinATerm() {
    var form = createValidForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid_withinAPhase() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);
    form.setLicenceSchedulePhaseId("licenceSchedulePhaseId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid_invalidForm_missingCategory() {
    var form = createValidForm();
    form.setOtherScheduleEventCategory(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingOtherCategoryName() {
    var form = createValidForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDescription() {
    var form = createValidForm();
    form.setDescription(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDateOption() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_relativeDateOption_missingEventId() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1,0,0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_relativeDateOption_missingDuration() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.setRelativeEventId(UUID.randomUUID().toString());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
    verify(scheduleRelativeDateValidationService, never()).validateRelativeDateBeforeEndOfSchedule(any(), any(), any(), any());
  }

  @Test
  void isValid_invalidForm_relativeDateOption_priorFieldError_doesNotCallRelativeDateValidation() {
    var form = createValidForm();
    form.setDescription(null);
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.setRelativeEventId(UUID.randomUUID().toString());
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
    verify(scheduleRelativeDateValidationService, never()).validateRelativeDateBeforeEndOfSchedule(any(), any(), any(), any());
  }

  @Test
  void isValid_invalidForm_termOption_missingTermId() {
    var form = createValidForm();
    form.setLicenceScheduleTermId(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalidForm_phaseOption_missingPhaseId() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "50", "87.5", "100", "100.0"})
  void isValid_relinquishmentPercentage_zeroToOneHundredToOneDecimalPlace(String percentage) {
    var form = createValidForm();
    form.getRelinquishmentPercentage().setInputValue(percentage);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void isValid_invalidForm_missingRelinquishmentPercentage(String percentage) {
    var form = createValidForm();
    form.getRelinquishmentPercentage().setInputValue(percentage);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult))
        .containsExactly(
            Map.entry("relinquishmentPercentage.inputValue", Set.of("Enter the mandatory relinquishment percentage"))
        );
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "-1     | The percentage must be between 0 and 100",
      "100.1  | The percentage must be between 0 and 100",
      "12.25  | The percentage can only have 1 decimal place",
      "x      | The percentage must be a number",
      "50%    | The percentage must be a number"
  })
  void isValid_invalidForm_relinquishmentPercentage(String percentage, String message) {
    var form = createValidForm();
    form.getRelinquishmentPercentage().setInputValue(percentage);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult))
        .containsExactly(Map.entry("relinquishmentPercentage.inputValue", Set.of(message)));
  }

  @Test
  void isValid_otherCategory_ignoresTheRelinquishmentPercentage() {
    var form = createValidForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);
    form.setOtherCategoryName("Drill-or-drop");
    form.getRelinquishmentPercentage().setInputValue("x");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  private OtherScheduleEventForm createValidForm() {
    OtherScheduleEventForm form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    form.getRelinquishmentPercentage().setInputValue("50");
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_TERM);
    form.setLicenceScheduleTermId("licenceScheduleTermId");
    return form;
  }

}