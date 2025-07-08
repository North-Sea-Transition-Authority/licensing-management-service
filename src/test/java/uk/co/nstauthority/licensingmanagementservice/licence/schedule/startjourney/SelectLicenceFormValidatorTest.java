package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class SelectLicenceFormValidatorTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private SelectLicenceFormValidator validator;

  @Test
  void isValid_validForm() {
    var form = new SelectLicenceForm();
    form.setLicenceId("1");

    var licence = new Licence();

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceScheduleService.doesLicenceScheduleExistForLicence(licence)).thenReturn(false);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noLicenceSelected() {
    var form = new SelectLicenceForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("licenceId", Set.of("licenceId.required")));
  }

  @Test
  void isValid_invalidForm_selectedLicenceAlreadyHasSchedule() {
    var form = new SelectLicenceForm();
    form.setLicenceId("1");

    var licence = new Licence();

    when(licenceService.findLicenceByIdOrThrow(1)).thenReturn(licence);
    when(licenceScheduleService.doesLicenceScheduleExistForLicence(licence)).thenReturn(true);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(validator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.getErrorsFieldsAndMessages(bindingResult))
        .containsExactly(entry("licenceId", List.of("A schedule already exists for the selected licence")));
  }

}