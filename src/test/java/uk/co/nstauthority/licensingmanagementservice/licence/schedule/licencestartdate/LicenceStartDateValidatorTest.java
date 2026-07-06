package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceStartDateValidatorTest {

  @InjectMocks
  private LicenceStartDateValidator licenceStartDateValidator;

  @Test
  void isValid() {
    var form = new LicenceStartDateForm();
    form.getLicenceStartDate().setDay(1);
    form.getLicenceStartDate().setMonth(1);
    form.getLicenceStartDate().setYear(2025);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceStartDateValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_notValid() {
    var form = new LicenceStartDateForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceStartDateValidator.isValid(form, bindingResult)).isFalse();
  }

  @Test
  void isValid_whenDateIsBeforeMinimumYear_thenReturnFalse() {
    var form = new LicenceStartDateForm();
    form.getLicenceStartDate().setDay(1);
    form.getLicenceStartDate().setMonth(1);
    form.getLicenceStartDate().setYear(1899);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceStartDateValidator.isValid(form, bindingResult)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult))
        .containsEntry("licenceStartDate.dayInput.inputValue", Set.of("The date must be after 1 January 1900"));
  }

  @Test
  void isValid_whenDateIsMinimumYear_thenReturnFalse() {
    var form = new LicenceStartDateForm();
    form.getLicenceStartDate().setDay(1);
    form.getLicenceStartDate().setMonth(1);
    form.getLicenceStartDate().setYear(1900);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceStartDateValidator.isValid(form, bindingResult)).isFalse();
    assertThat(ValidatorTestingUtil.extractErrorMessages(bindingResult))
        .containsEntry("licenceStartDate.dayInput.inputValue", Set.of("The date must be after 1 January 1900"));
  }
}