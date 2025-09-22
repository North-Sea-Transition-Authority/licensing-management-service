package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleExtensionFormValidatorTest {

  @InjectMocks
  private LicenceScheduleExtensionFormValidator licenceScheduleExtensionFormValidator;

  @Test
  void isValid() {
    var form = new LicenceScheduleExtensionForm();
    form.setExplanation("test");
    form.setExtensionDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void validationInvalidExplanation() {
    var form = new LicenceScheduleExtensionForm();
    form.setExplanation("");
    form.setExtensionDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void validationInvalidDuration() {
    var form = new LicenceScheduleExtensionForm();
    form.setExplanation("testExplanation");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleExtensionFormValidator.isValid(form, bindingResult)).isFalse();
  }

  private ThreeFieldDurationInput getValidDuration() {
    var durationInput = new ThreeFieldDurationInput("phaseDuration", "phase");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");

    return durationInput;
  }
}