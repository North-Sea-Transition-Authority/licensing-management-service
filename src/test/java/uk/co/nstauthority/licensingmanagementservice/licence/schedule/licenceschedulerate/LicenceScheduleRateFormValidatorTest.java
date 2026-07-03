package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleRateFormValidatorTest {

  @InjectMocks
  private LicenceScheduleRateFormValidator validator;

  @Test
  void isValid_termOption() {
    var form = createValidForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_phaseOption() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.PHASE);
    form.setLicenceSchedulePhaseId("licenceSchedulePhaseId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_customPeriodOption_startOnEventStartDate() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);
    form.setRelativeEventId("relativeEventId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_customPeriodOption_startRelativeToEventStartDate() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    form.setRelativeEventId("relativeEventId");
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_missingRateDefinitionOption() {
    var form = createValidForm();
    form.setRateDefinitionOption(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_termOption_missingTermId() {
    var form = createValidForm();
    form.setLicenceScheduleTermId(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_phaseOption_missingPhaseId() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.PHASE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_customPeriodOption_missingRelativeDateOption() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.setRelativeEventId("relativeEventId");
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1, 0, 0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_customPeriodOption_missingRelativeEventId() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.setRateRelativeDateOption(RateRelativeDateOption.ON_START_DATE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_customPeriodOption_startRelativeToEventStartDate_missingRelativeDuration() {
    var form = createValidForm();
    form.setRateDefinitionOption(RateDefinitionOption.CUSTOM_PERIOD);
    form.setRateRelativeDateOption(RateRelativeDateOption.RELATIVE_TO_START_DATE);
    form.setRelativeEventId("relativeEventId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingRentalRate() {
    var form = createValidForm();
    form.getRentalRate().setInputValue("");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_rentalRateInvalidInput() {
    var form = createValidForm();
    form.getRentalRate().setInputValue("text");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_negativeRentalRate() {
    var form = createValidForm();
    form.getRentalRate().setInputValue("-1.00");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_rentalRateWithNoDecimalPlaces() {
    var form = createValidForm();
    form.getRentalRate().setInputValue("1");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_rentalRateWithOneDecimalPlace() {
    var form = createValidForm();
    form.getRentalRate().setInputValue("1.5");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_rentalRateTooManyDecimalPlaces() {
    var form = createValidForm();
    form.getRentalRate().setInputValue("1.234");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  private LicenceScheduleRateForm createValidForm() {
    var licenceScheduleRateForm = new LicenceScheduleRateForm();
    licenceScheduleRateForm.setRateDefinitionOption(RateDefinitionOption.TERM);
    licenceScheduleRateForm.setLicenceScheduleTermId("licenceScheduleTermId");
    licenceScheduleRateForm.getRentalRate().setInputValue("1.00");

    return licenceScheduleRateForm;
  }
}