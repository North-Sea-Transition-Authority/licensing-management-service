package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseFormServiceTest {

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @Mock
  private LicenceScheduleCalculationService licenceScheduleCalculationService;

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

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail);

    verify(licenceSchedulePhaseRepository).save(licenceSchedulePhaseArgumentCaptor.capture());

    var result = licenceSchedulePhaseArgumentCaptor.getValue();

    assertThat(result).extracting(
        LicenceSchedulePhase::getLicenceScheduleDetail,
        LicenceSchedulePhase::getPhaseType,
        LicenceSchedulePhase::getPhaseDuration,
        LicenceSchedulePhase::getComments,
        LicenceSchedulePhase::getStatus
    ).containsExactly(
        licenceScheduleDetail,
        PhaseType.PHASE_A,
        form.getPhaseDuration().toThreeFieldDuration(),
        form.getComments(),
        LicenceScheduleEventStatus.ACTIVE
    );

    verify(licenceScheduleCalculationService).calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }
}