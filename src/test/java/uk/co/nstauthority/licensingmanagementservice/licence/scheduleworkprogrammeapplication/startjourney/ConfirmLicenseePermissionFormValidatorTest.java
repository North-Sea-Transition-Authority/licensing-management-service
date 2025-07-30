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
class ConfirmLicenseePermissionFormValidatorTest {

  @InjectMocks
  private ConfirmLicenseePermissionFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = new ConfirmLicenseePermissionForm();
    form.setAllLicenseesPermissionConfirmed(true);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_NoValueForallLicenseesPermissionConfirmed() {
    var form = new ConfirmLicenseePermissionForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("allLicenseesPermissionConfirmed", Set.of("allLicenseesPermissionConfirmed.required")));
  }
}