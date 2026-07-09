package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityDateOption;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceWorkProgrammeAmendmentFormValidatorTest {

  @InjectMocks
  private LicenceWorkProgrammeAmendmentFormValidator licenceWorkProgrammeAmendmentFormValidator;

  @Test
  void isValid_whenNotRelativeDate_andAmendmentInformationProvided_thenValid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setWorkProgrammeAmendmentInformation("testInformation");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM))).isTrue();
  }

  @Test
  void isValid_whenNotRelativeDate_andAmendmentInformationEmpty_thenInvalid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setWorkProgrammeAmendmentInformation("");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM))).isFalse();
  }

  @Test
  void isValid_whenNotRelativeDate_andExtensionAndAmendmentInformationProvided_thenValid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(true);
    form.setWorkProgrammeAmendmentInformation("testInformation");
    form.setWorkProgrammeExtensionDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM))).isTrue();
  }

  @Test
  void isValid_whenNotRelativeDate_andExtensionRequested_andDurationEmpty_thenInvalid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(true);
    form.setWorkProgrammeAmendmentInformation("testInformation");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM))).isFalse();
  }

  @Test
  void isValid_whenRelativeDate_andNeitherOptionSelected_thenInvalid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(false);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE))).isFalse();
  }

  @Test
  void isValid_whenRelativeDate_andExtensionOnlyWithValidDuration_thenValid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(true);
    form.setAdditionalInfoRequired(false);
    form.setWorkProgrammeExtensionDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE))).isTrue();
  }

  @Test
  void isValid_whenRelativeDate_andAdditionalInfoRequestedWithInformation_thenValid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(true);
    form.setWorkProgrammeAmendmentInformation("testInformation");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE))).isTrue();
  }

  @Test
  void isValid_whenRelativeDate_andAdditionalInfoRequested_andInformationEmpty_thenInvalid() {
    var form = new LicenceWorkProgrammeAmendmentForm();
    form.setDurationExtensionRequired(false);
    form.setAdditionalInfoRequired(true);
    form.setWorkProgrammeAmendmentInformation("");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceWorkProgrammeAmendmentFormValidator.isValid(form, bindingResult, activityWithDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE))).isFalse();
  }

  private WorkProgrammeActivity activityWithDateOption(WorkProgrammeActivityDateOption dateOption) {
    var activity = new WorkProgrammeActivity();
    activity.setDateOption(dateOption);
    return activity;
  }

  private ThreeFieldDurationInput getValidDuration() {
    var durationInput = new ThreeFieldDurationInput("extensionDuration", "extensionDuration");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");
    return durationInput;
  }
}
