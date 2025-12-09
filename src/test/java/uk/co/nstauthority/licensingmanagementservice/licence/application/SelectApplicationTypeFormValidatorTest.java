package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SelectApplicationTypeFormValidatorTest {

  @InjectMocks
  private SelectApplicationTypeFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = new SelectApplicationTypeForm();
    form.setSelectedApplicationType(ApplicationType.CONTINUATION_APPLICATION);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noTypeSelected() {
    var form = new SelectApplicationTypeForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedApplicationType", Set.of("selectedApplicationType.required")));
  }

}