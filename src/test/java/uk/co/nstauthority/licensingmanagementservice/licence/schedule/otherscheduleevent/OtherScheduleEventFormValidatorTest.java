package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class OtherScheduleEventFormValidatorTest {

  @InjectMocks
  private OtherScheduleEventFormValidator validator;

  @Test
  void isValid_relativeDate() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.setRelativeEventId("relativeEventId");
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1,0,0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_withinATerm() {
    var form = createValidForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_withinAPhase() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);
    form.setLicenceSchedulePhaseId("licenceSchedulePhaseId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_missingCategory() {
    var form = createValidForm();
    form.setOtherScheduleEventCategory(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingOtherCategoryName() {
    var form = createValidForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.OTHER_ACTIVITY);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDescription() {
    var form = createValidForm();
    form.setDescription(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDateOption() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_relativeDateOption_missingEventId() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1,0,0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_relativeDateOption_missingDuration() {
    var form = createValidForm();
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.RELATIVE_DATE);
    form.setRelativeEventId("relativeEventId");

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
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_PHASE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  private OtherScheduleEventForm createValidForm() {
    OtherScheduleEventForm form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(OtherScheduleEventCategory.MANDATORY_RELINQUISHMENT);
    form.setDescription("description");
    form.setOtherScheduleEventDateOption(OtherScheduleEventDateOption.WITHIN_A_TERM);
    form.setLicenceScheduleTermId("licenceScheduleTermId");
    return form;
  }

}