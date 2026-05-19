package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class LicenceSchedulePhaseServiceTest {

  @Mock
  private LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  @InjectMocks
  private LicenceSchedulePhaseService licenceSchedulePhaseService;

  @Test
  void getPhaseByIdOrThrow() {
    var licenceSchedulePhase = new LicenceSchedulePhase();
    licenceSchedulePhase.setId(UUID.randomUUID());

    when(licenceSchedulePhaseRepository.findById(licenceSchedulePhase.getId())).thenReturn(Optional.of(licenceSchedulePhase));

    assertThat(licenceSchedulePhaseService.getPhaseByIdOrThrow(licenceSchedulePhase.getId())).isEqualTo(licenceSchedulePhase);
  }

  @Test
  void getPhaseByIdOrThrow_phaseNotFound() {
    when(licenceSchedulePhaseRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceSchedulePhaseService.getPhaseByIdOrThrow(UUID.randomUUID()))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getActivePhasesByLicenceScheduleDetail() {
    var licenceScheduleDetail = new LicenceScheduleDetail();

    licenceSchedulePhaseService.getActivePhasesByLicenceScheduleDetail(licenceScheduleDetail);

    verify(licenceSchedulePhaseRepository).findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Test
  void saveLicenceSchedulePhases() {
    var phases = List.of(new LicenceSchedulePhase());

    licenceSchedulePhaseService.saveLicenceSchedulePhases(phases);

    verify(licenceSchedulePhaseRepository).saveAll(phases);
  }

  @Test
  void getActivePhasesByTerm() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    licenceSchedulePhaseService.getActivePhasesByTerm(licenceScheduleTerm);

    verify(licenceSchedulePhaseRepository).findAllByLicenceScheduleTerm(licenceScheduleTerm);
  }

  @Test
  void deletePhase() {
    var licenceSchedulePhase = new LicenceSchedulePhase();

    licenceSchedulePhaseService.deletePhase(licenceSchedulePhase);

    verify(licenceSchedulePhaseRepository).delete(licenceSchedulePhase);
  }
}
