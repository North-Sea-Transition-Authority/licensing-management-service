package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class SelectApplicationLicenceTypeFormValidatorTest {

  private final SelectApplicationLicenceTypeFormValidator validator = new SelectApplicationLicenceTypeFormValidator();

  @Test
  void isValid_validForm() {
    var form = new SelectApplicationLicenceTypeForm();
    form.setSelectedLicenceType(LicenceType.CARBON_STORAGE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, List.of(LicenceType.CARBON_STORAGE))).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicenceTypeSelected() {
    var form = new SelectApplicationLicenceTypeForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, List.of(LicenceType.CARBON_STORAGE))).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedLicenceType", Set.of("selectedLicenceType.required")));
  }

  @Test
  void isValid_invalidForm_licenceTypeNotAvailable() {
    var form = new SelectApplicationLicenceTypeForm();
    form.setSelectedLicenceType(LicenceType.SEAWARD_EXPLORATION);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, List.of(LicenceType.CARBON_STORAGE))).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedLicenceType", Set.of("selectedLicenceType.notAvailable")));
  }
}
