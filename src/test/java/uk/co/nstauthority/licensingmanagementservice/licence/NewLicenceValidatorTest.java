package uk.co.nstauthority.licensingmanagementservice.licence;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class NewLicenceValidatorTest {

  @InjectMocks
  private NewLicenceValidator newLicenceValidator;

  @Test
  void isValid() {
    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.CARBON_STORAGE);
    form.setLicenceNumber("001");
    form.setOrganisationUnitIds(List.of("1"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicenceType() {
    var form = new NewLicenceForm();
    form.setLicenceNumber("001");
    form.setOrganisationUnitIds(List.of("1"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceType", Set.of("licenceType.required")));
  }

  @Test
  void isValid_invalidForm_noLicenceNumber() {
    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.CARBON_STORAGE);
    form.setOrganisationUnitIds(List.of("1"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceNumber", Set.of("licenceNumber.required")));
  }

  @Test
  void isValid_invalidForm_invalidLicenceNumber() {
    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.CARBON_STORAGE);
    form.setLicenceNumber("CS001");
    form.setOrganisationUnitIds(List.of("1"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceNumber", Set.of("licenceNumber.invalid")));
  }

  @Test
  void isValid_invalidForm_noLicensees() {
    var form = new NewLicenceForm();
    form.setLicenceType(LicenceType.CARBON_STORAGE);
    form.setLicenceNumber("001");

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(newLicenceValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("organisationUnitSelector", Set.of("organisationUnitSelector.notEmpty")));
  }
}