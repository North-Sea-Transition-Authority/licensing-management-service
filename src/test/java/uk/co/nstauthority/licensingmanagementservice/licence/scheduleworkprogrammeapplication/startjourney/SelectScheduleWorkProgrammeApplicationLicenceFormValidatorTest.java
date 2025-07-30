package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SelectScheduleWorkProgrammeApplicationLicenceFormValidatorTest {

  @InjectMocks
  private SelectScheduleWorkProgrammeApplicationLicenceFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = new SelectScheduleWorkProgrammeApplicationLicenceForm();
    form.setLicenceId("1");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicenceSelected() {
    var form = new SelectScheduleWorkProgrammeApplicationLicenceForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceId", Set.of("licenceId.required")));
  }

}