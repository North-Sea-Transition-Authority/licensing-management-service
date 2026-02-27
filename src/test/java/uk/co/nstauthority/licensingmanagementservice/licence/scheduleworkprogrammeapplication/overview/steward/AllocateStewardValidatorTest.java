package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class AllocateStewardValidatorTest {

  @InjectMocks
  private AllocateStewardValidator allocateStewardValidator;

  @Test
  void isValid_validForm_returnsTrue() {
    var form = new AllocateStewardForm();
    form.setStewardWuaId(String.valueOf(1L));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    var stewardOptions = Map.of(String.valueOf(1L), "Jane Doe");

    assertThat(allocateStewardValidator.isValid(form, bindingResult, stewardOptions)).isTrue();
    assertThat(bindingResult.hasErrors()).isFalse();
  }

  @Test
  void isValid_nullStewardWuaId_returnsFalse() {
    var form = new AllocateStewardForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    var stewardOptions = Map.of(String.valueOf(1L), "Jane Doe");

    assertThat(allocateStewardValidator.isValid(form, bindingResult, stewardOptions)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("stewardWuaId", Set.of("stewardWuaId.required")));
  }

  @Test
  void isValid_wuaIdNotInOptions_returnsFalse() {
    var form = new AllocateStewardForm();
    form.setStewardWuaId(String.valueOf(999L));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);
    var stewardOptions = Map.of(String.valueOf(1L), "Jane Doe");

    assertThat(allocateStewardValidator.isValid(form, bindingResult, stewardOptions)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("stewardWuaId", Set.of("stewardWuaId.invalid")));
  }
}
