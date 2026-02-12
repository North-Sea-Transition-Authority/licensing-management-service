package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationLicenseeInformationFormValidatorTest {

  @InjectMocks
  private LicenceContinuationLicenseeInformationFormValidator validator;

  @Test
  void isValid_validForm() {

    var form = validForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    assertThat(validator.isValid(bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_NoValueForResponsibleOrganisationUnitId() {

    var form = validForm();
    form.setResponsibleOrganisationUnitId(null);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("responsibleOrganisationUnitId", Set.of("responsibleOrganisationUnitId.required")));
  }

  private LicenceContinuationLicenseeInformationForm validForm() {

    var form = new LicenceContinuationLicenseeInformationForm();
    form.setResponsibleOrganisationUnitId(1);
    return form;
  }
}