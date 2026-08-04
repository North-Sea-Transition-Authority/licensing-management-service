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
class EditLicenceDetailsValidatorTest {

  @InjectMocks
  private EditLicenceDetailsValidator editLicenceDetailsValidator;

  @Test
  void isValid() {
    var form = new EditLicenceDetailsForm();

    form.setOrganisationUnitIds(List.of("1"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicensees() {
    var form = new EditLicenceDetailsForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(editLicenceDetailsValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("organisationUnitSelector", Set.of("organisationUnitSelector.notEmpty")));
  }

}