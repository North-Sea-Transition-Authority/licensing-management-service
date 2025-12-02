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
class LicenseeInformationFormValidatorTest {

  @InjectMocks
  private LicenseeInformationFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = validForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_NoValueForallLicenseesPermissionConfirmed() {
    var form = validForm();
    form.setAllLicenseesPermissionConfirmed(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("allLicenseesPermissionConfirmed", Set.of("allLicenseesPermissionConfirmed.required")));
  }

  @Test
  void isValid_invalidForm_NoValueForResponsibleOrganisationUnitId() {
    var form = validForm();
    form.setResponsibleOrganisationUnitId(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("responsibleOrganisationUnitId", Set.of("responsibleOrganisationUnitId.required")));
  }

  private LicenseeInformationForm validForm() {
    var form = new LicenseeInformationForm();
    form.setAllLicenseesPermissionConfirmed(true);
    form.setResponsibleOrganisationUnitId(1);
    return form;
  }
}