package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDurationInput;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleTermFormValidatorTest {

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @InjectMocks
  private LicenceScheduleTermFormValidator licenceScheduleTermFormValidator;

  @Test
  void isValid() {
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isTrue();
  }

  @Test
  void isValid_invalid_termTypeNotSelected() {
    var form = new LicenceScheduleTermForm();
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalid_termTypeAlreadyExists() {
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalid_invalidDuration() {
    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(new ThreeFieldDurationInput("termDuration", "term"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValidUpdate() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.INITIAL);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceScheduleTerm)).isTrue();
  }

  @Test
  void isValidUpdate_invalid_termTypeAlreadyExists() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setId(UUID.randomUUID());
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    var licenceScheduleTerm2 = new LicenceScheduleTerm();
    licenceScheduleTerm2.setId(UUID.randomUUID());
    licenceScheduleTerm2.setTermType(TermType.SECOND);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm, licenceScheduleTerm2));

    var form = new LicenceScheduleTermForm();
    form.setTermType(TermType.SECOND);
    form.setTermDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceScheduleTermFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceScheduleTerm)).isFalse();
  }

  private ThreeFieldDurationInput getValidDuration() {
    var durationInput = new ThreeFieldDurationInput("termDuration", "term");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");

    return durationInput;
  }

}