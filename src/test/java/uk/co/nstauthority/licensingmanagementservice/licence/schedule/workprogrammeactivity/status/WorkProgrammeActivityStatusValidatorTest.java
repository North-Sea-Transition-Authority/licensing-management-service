package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
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

    assertThat(workProgrammeActivityStatusValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_missingStatus() {
    var form = new WorkProgrammeActivityStatusForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_isTransferred_missingTransferredToLicenceId() {
    var form = new WorkProgrammeActivityStatusForm();
    form.setStatus(WorkProgrammeStatus.TRANSFERRED);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(workProgrammeActivityStatusValidator.isValid(form, bindingResult)).isFalse();
  }
}