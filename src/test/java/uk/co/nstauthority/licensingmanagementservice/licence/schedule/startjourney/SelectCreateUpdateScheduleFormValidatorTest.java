package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SelectCreateUpdateScheduleFormValidatorTest {

  @InjectMocks
  private SelectCreateUpdateScheduleFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = new SelectCreateUpdateScheduleForm();
    form.setSelectedJourneyOption(ScheduleJourneyOption.CREATE);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm() {
    var form = new SelectCreateUpdateScheduleForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("selectedJourneyOption", Set.of("selectedJourneyOption.required")));
  }
}