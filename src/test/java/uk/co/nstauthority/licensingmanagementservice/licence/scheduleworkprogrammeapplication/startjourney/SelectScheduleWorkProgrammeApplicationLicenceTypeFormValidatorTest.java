package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SelectScheduleWorkProgrammeApplicationLicenceTypeFormValidatorTest {

  @InjectMocks
  private SelectScheduleWorkProgrammeApplicationLicenceTypeFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = new SelectScheduleWorkProgrammeApplicationLicenceTypeForm();
    form.setSelectedLicenceType(LicenceType.CARBON_STORAGE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm() {
    var form = new SelectScheduleWorkProgrammeApplicationLicenceTypeForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedLicenceType", Set.of("selectedLicenceType.required")));
  }
}