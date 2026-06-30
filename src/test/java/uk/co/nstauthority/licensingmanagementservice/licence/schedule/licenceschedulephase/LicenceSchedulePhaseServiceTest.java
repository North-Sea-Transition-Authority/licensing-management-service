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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
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
  void getPhasesByTerm() {
    var licenceScheduleTerm = new LicenceScheduleTerm();

    licenceSchedulePhaseService.getPhasesByTerm(licenceScheduleTerm);

    verify(licenceSchedulePhaseRepository).findAllByLicenceScheduleTerm(licenceScheduleTerm);
  }

  @Test
  void getPhaseByScheduleDetailAndEventReferenceOrThrow() {
    var detail = new LicenceScheduleDetail();
    var eventReference = new EventReference();
    var phase = new LicenceSchedulePhase();

    when(licenceSchedulePhaseRepository.findByLicenceScheduleDetailAndEventReference(detail, eventReference))
        .thenReturn(Optional.of(phase));

    assertThat(licenceSchedulePhaseService.getPhaseByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isEqualTo(phase);
  }

  @Test
  void getPhaseByScheduleDetailAndEventReferenceOrThrow_notFound() {
    var eventReference = new EventReference();
    eventReference.setId(UUID.randomUUID());

    when(licenceSchedulePhaseRepository.findByLicenceScheduleDetailAndEventReference(any(), any()))
        .thenReturn(Optional.empty());

    var detail = new LicenceScheduleDetail();

    assertThatThrownBy(() -> licenceSchedulePhaseService.getPhaseByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void deletePhase() {
    var licenceSchedulePhase = new LicenceSchedulePhase();

    licenceSchedulePhaseService.deletePhase(licenceSchedulePhase);

    verify(licenceSchedulePhaseRepository).delete(licenceSchedulePhase);
  }
}
