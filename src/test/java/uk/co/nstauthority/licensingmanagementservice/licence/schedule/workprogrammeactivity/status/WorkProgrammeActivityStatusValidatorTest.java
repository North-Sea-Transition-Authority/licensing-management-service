package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityStatusValidatorTest {

  @InjectMocks
  private WorkProgrammeActivityStatusValidator workProgrammeActivityStatusValidator;

  @Test
  void isValid() {
    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.OPEN);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, activity(WorkProgrammeActivityCategory.WELL_TEST), bindingResult)).isTrue();
  }

  @Test
  void isValid_missingStatus() {
    var form = new WorkProgrammeActivityStatusForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, activity(WorkProgrammeActivityCategory.WELL_TEST), bindingResult)).isFalse();
  }

  @Test
  void isValid_isTransferred_missingTransferredToLicenceId() {
    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.TRANSFERRED);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, activity(WorkProgrammeActivityCategory.WELL_TEST), bindingResult)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(value = WorkProgrammeActivityCategory.class, names = {"DRILL_OR_DROP_WELL", "OTHER_ACTIVITY"})
  void isValid_droppedOnADrillOrDropWellOrOtherActivity(WorkProgrammeActivityCategory category) {
    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.DROPPED);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, activity(category), bindingResult)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = WorkProgrammeActivityCategory.class, names = {"DRILL_WELL", "NEW_SHOOT_3_D_SEISMIC_DATA", "WELL_TEST"})
  void isValid_droppedOnAnyOtherCategory(WorkProgrammeActivityCategory category) {
    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.DROPPED);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, activity(category), bindingResult)).isFalse();
    assertThat(bindingResult.getFieldError("status")).isNotNull();
  }

  private static WorkProgrammeActivity activity(WorkProgrammeActivityCategory category) {
    var activity = new WorkProgrammeActivity();
    activity.setCategory(category);
    return activity;
  }
}
