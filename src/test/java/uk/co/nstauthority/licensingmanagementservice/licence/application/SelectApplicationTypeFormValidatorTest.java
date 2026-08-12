package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class SelectApplicationTypeFormValidatorTest {

  private SelectApplicationTypeFormValidator validatorWithProfiles(String... activeProfiles) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles(activeProfiles);
    return new SelectApplicationTypeFormValidator(new FeatureFlagService(environment));
  }

  @Test
  void isValid_validForm() {
    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(ApplicationType.CONTINUATION_APPLICATION);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validatorWithProfiles("enable-lms1").isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noTypeSelected() {
    var form = new SelectApplicationTypeForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validatorWithProfiles("enable-lms1").isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedApplicationType", Set.of("selectedApplicationType.required")));
  }

  @Test
  void isValid_invalidForm_typeNotAvailableInCurrentPhase() {
    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(ApplicationType.CONTINUATION_APPLICATION);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    // No phase profiles active -> the type's feature is off -> rejected on submit
    assertThat(validatorWithProfiles().isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedApplicationType", Set.of("selectedApplicationType.notAvailable")));
  }

}
