package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeActivityFormValidatorTest {

  @InjectMocks
  private WorkProgrammeActivityFormValidator validator;

  @Test
  void isValid_relativeDate() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
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
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);
    form.setLicenceSchedulePhaseId("licenceSchedulePhaseId");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_missingCategory() {
    var form = createValidForm();
    form.setWorkProgrammeActivityCategory(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingOtherCategoryName() {
    var form = createValidForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.OTHER_ACTIVITY);

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
  void isValid_invalidForm_missingCommitment() {
    var form = createValidForm();
    form.setWorkProgrammeActivityCommitment(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_missingDateOption() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_relativeDateOption_missingEventId() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
    form.getRelativeDuration().setFromThreeFieldDuration(new ThreeFieldDuration(1,0,0));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_invalidForm_relativeDateOption_missingDuration() {
    var form = createValidForm();
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.RELATIVE_DATE);
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
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_PHASE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();
  }

  private WorkProgrammeActivityForm createValidForm() {
    WorkProgrammeActivityForm form = new WorkProgrammeActivityForm();
    form.setWorkProgrammeActivityCategory(WorkProgrammeActivityCategory.WELL_TEST);
    form.setDescription("description");
    form.setWorkProgrammeActivityCommitment(WorkProgrammeActivityCommitment.FIRM);
    form.setWorkProgrammeActivityDateOption(WorkProgrammeActivityDateOption.WITHIN_A_TERM);
    form.setLicenceScheduleTermId("licenceScheduleTermId");
    return form;
  }

}