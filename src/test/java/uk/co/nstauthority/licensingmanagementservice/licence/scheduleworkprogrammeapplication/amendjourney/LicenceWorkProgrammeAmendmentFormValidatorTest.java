package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentFormValidatorTest {

  @InjectMocks
  private LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  @Test
  void isValidNoExtension() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(true);
    form.setWorkProgrammeAmendmentInformation("testInformation");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)).isTrue();
  }


  @Test
  void isValidNoExtensionAndAmendmentInformation() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(false);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);
    form.setWorkProgrammeAmendmentInformation("testInformation");
    form.setWorkProgrammeExtensionDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void validationInvalidExplanation() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);
    form.setWorkProgrammeAmendmentInformation("");
    form.setWorkProgrammeExtensionDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void validationInvalidDuration() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(true);
    form.setWorkProgrammeAmendmentInformation("testInformation");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult)).isFalse();
  }

  private ThreeFieldDurationInput getValidDuration() {
    var durationInput = new ThreeFieldDurationInput("extensionDuration", "extensionDuration");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");

    return durationInput;
  }
}