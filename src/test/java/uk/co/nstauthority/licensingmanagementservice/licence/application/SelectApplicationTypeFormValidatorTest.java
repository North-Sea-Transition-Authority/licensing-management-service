package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

class SelectApplicationTypeFormValidatorTest {

  private final SelectApplicationTypeFormValidator validator = new SelectApplicationTypeFormValidator();

  @Test
  void isValid_validForm() {
    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(ApplicationType.CONTINUATION_APPLICATION);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, List.of(ApplicationType.CONTINUATION_APPLICATION))).isTrue();
  }

  @Test
  void isValid_invalidForm_noTypeSelected() {
    var form = new SelectApplicationTypeForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, List.of(ApplicationType.CONTINUATION_APPLICATION))).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedApplicationType", Set.of("selectedApplicationType.required")));
  }

  @Test
  void isValid_invalidForm_typeNotAvailable() {
    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(ApplicationType.CONTINUATION_APPLICATION);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult, List.of(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION))).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedApplicationType", Set.of("selectedApplicationType.notAvailable")));
  }

}
