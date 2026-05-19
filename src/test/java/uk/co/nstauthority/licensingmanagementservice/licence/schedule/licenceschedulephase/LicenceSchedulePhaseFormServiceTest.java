package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseFormServiceTest {

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

  @Mock
  private LicenceScheduleTermService licenceScheduleTermService;

  @InjectMocks
  private LicenceSchedulePhaseFormService licenceSchedulePhaseFormService;

  @Captor
  private ArgumentCaptor<LicenceSchedulePhase> licenceSchedulePhaseArgumentCaptor;

  @Test
  void savePhaseFromForm() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setYears("1");
    form.getPhaseDuration().setMonths("0");
    form.getPhaseDuration().setDays("0");
    form.setComments("comments");

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail, new LicenceSchedulePhase());

    verify(licenceSchedulePhaseRepository).save(licenceSchedulePhaseArgumentCaptor.capture());

    var result = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getPhaseType,
        LicenceSchedulePhase::getPhaseDuration,
        LicenceSchedulePhase::getComments,
        LicenceSchedulePhase::getLicenceScheduleTerm
    ).containsExactly(
        licenceScheduleDetail,
        PhaseType.PHASE_A,
        form.getPhaseDuration().toThreeFieldDuration(),
        form.getComments(),
        term
    );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  @Test
  void savePhaseFromForm_existingPhase_doesntOverwriteEventReference() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    var form = new LicenceSchedulePhaseForm();
    form.setPhaseType(PhaseType.PHASE_A);
    form.getPhaseDuration().setYears("1");
    form.getPhaseDuration().setMonths("0");
    form.getPhaseDuration().setDays("0");
    form.setComments("comments");

    var term = new LicenceScheduleTerm();
    term.setTermType(TermType.INITIAL);

    when(licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail)).thenReturn(List.of(term));

    var phase = new LicenceSchedulePhase();
    phase.setEventReference(UUID.randomUUID());

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail, phase);

    verify(licenceSchedulePhaseRepository).save(licenceSchedulePhaseArgumentCaptor.capture());

    var result = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getPhaseType,
        LicenceSchedulePhase::getPhaseDuration,
        LicenceSchedulePhase::getComments,
        LicenceSchedulePhase::getLicenceScheduleTerm,
        LicenceSchedulePhase::getEventReference
    ).containsExactly(
        licenceScheduleDetail,
        PhaseType.PHASE_A,
        form.getPhaseDuration().toThreeFieldDuration(),
        form.getComments(),
        term,
        phase.getEventReference()
    );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }
}
