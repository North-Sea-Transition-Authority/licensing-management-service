package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SwpApplicationRequestPurposeValidatorTest {

  @InjectMocks
  private SwpApplicationRequestPurposeValidator swpApplicationRequestPurposeValidator;

  @Test
  void isValid_invalidForm() {
    var form = new SwpApplicationRequestPurposeForm();
    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(swpApplicationRequestPurposeValidator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_validForm() {
    var form = new SwpApplicationRequestPurposeForm();
    form.setRequestPurposes(Set.of(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(swpApplicationRequestPurposeValidator.isValid(form, bindingResult)).isTrue();
  }
}