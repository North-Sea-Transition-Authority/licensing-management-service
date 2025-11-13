package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

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
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseFormValidatorTest {

  @Mock
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @InjectMocks
  private LicenceSchedulePhaseFormValidator licenceSchedulePhaseFormValidator;

  @Test
  void isValid() {
    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.setPhaseDuration(getValidDuration());

    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isTrue();
  }

  @Test
  void isValid_invalid_phaseTypeNotSelected() {
    var form = new LicenceSchedulePhaseForm();
    form.setPhaseDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValid_invalid_missingTermTypeOnSchedule() {
    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.setPhaseDuration(getValidDuration());

    var licenceScheduleDetail = new LicenceScheduleDetail();

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalid_phaseTypeAlreadyExists() {
    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.setPhaseDuration(getValidDuration());

    var licenceScheduleDetail = new LicenceScheduleDetail();

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));
    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceSchedulePhase));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValid(form, bindingResult, licenceScheduleDetail)).isFalse();
  }

  @Test
  void isValid_invalid_invalidDuration() {
    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.setPhaseDuration(new ThreeFieldDurationInput("phaseDuration", "phase"));

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValid(form, bindingResult, new LicenceScheduleDetail())).isFalse();
  }

  @Test
  void isValidUpdate() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setId(UUID.randomUUID());
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceSchedulePhase));

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.setPhaseDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceSchedulePhase)).isTrue();
  }

  @Test
  void isValidUpdate_invalid_termTypeAlreadyExists() {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setId(UUID.randomUUID());
    licenceSchedulePhase.setPhaseType(PhaseType.PHASE_A);

    var licenceSchedulePhase2 = new LicenceSchedulePhase();
    licenceSchedulePhase2.setId(UUID.randomUUID());
    licenceSchedulePhase2.setPhaseType(PhaseType.PHASE_B);

    when(licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceSchedulePhase, licenceSchedulePhase2));

    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(licenceScheduleTerm));

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_B);
    form.setPhaseDuration(getValidDuration());

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceSchedulePhaseFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, licenceSchedulePhase)).isFalse();
  }

  private ThreeFieldDurationInput getValidDuration() {
    var durationInput = new ThreeFieldDurationInput("phaseDuration", "phase");
    durationInput.setYears("1");
    durationInput.setMonths("1");
    durationInput.setDays("1");

    return durationInput;
  }

}