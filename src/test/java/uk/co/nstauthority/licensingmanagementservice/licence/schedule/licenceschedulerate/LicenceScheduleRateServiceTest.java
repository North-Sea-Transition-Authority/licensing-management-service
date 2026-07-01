package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleRateServiceTest {

  @Mock
  private LicenceScheduleRateRepository licenceScheduleRateRepository;

  @InjectMocks
  private LicenceScheduleRateService licenceScheduleRateService;

  private final LicenceScheduleTerm term = new LicenceScheduleTerm();
  private final LicenceSchedulePhase phase = new LicenceSchedulePhase();
  private final LicenceScheduleRate rate = new LicenceScheduleRate();

  @Test
  void getRateByIdOrThrow() {
    rate.setId(UUID.randomUUID());

    when(licenceScheduleRateRepository.findById(rate.getId())).thenReturn(Optional.of(rate));

    assertThat(licenceScheduleRateService.getRateByIdOrThrow(rate.getId())).isEqualTo(rate);
  }

  @Test
  void getRateByIdOrThrow_activityNotFound() {
    when(licenceScheduleRateRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> licenceScheduleRateService.getRateByIdOrThrow(UUID.randomUUID()))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getLicenceScheduleRates() {
    var detail = new LicenceScheduleDetail();

    licenceScheduleRateService.getLicenceScheduleRates(detail);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetail(detail);
  }

  @Test
  void getLicenceScheduleRatesAttachedToTerm() {
    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getLicenceScheduleRatesAttachedToTerm(term)).isEqualTo(List.of(rate));
  }

  @Test
  void getLicenceScheduleRatesByTerm_forTerm() {
    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getLicenceScheduleRatesByTerm(term)).isEqualTo(List.of(rate));
  }

  @Test
  void getLicenceScheduleRatesByTerm_termDateRange() {
    term.setLicenceScheduleDetail(new LicenceScheduleDetail());
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of());

    licenceScheduleRateService.getLicenceScheduleRatesByTerm(term);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetailAndStartDateBetween(
        term.getLicenceScheduleDetail(),
        term.getStartDate(),
        term.getEndDate()
    );
  }

  @Test
  void getLicenceScheduleRatesByPhase_forTerm() {
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setLicenceScheduleTerm(term);

    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).isEqualTo(List.of());
  }

  @Test
  void getLicenceScheduleRatesByPhase_forPhase() {
    phase.setPhaseType(PhaseType.PHASE_B);

    when(licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(phase, RateDefinitionOption.PHASE))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).isEqualTo(List.of(rate));
  }

  @Test
  void getLicenceScheduleRatesByPhase_phaseDateRange() {
    phase.setLicenceScheduleDetail(new LicenceScheduleDetail());
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setPhaseType(PhaseType.PHASE_B);

    when(licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(phase, RateDefinitionOption.PHASE))
        .thenReturn(List.of());

    licenceScheduleRateService.getLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetailAndStartDateBetween(
        phase.getLicenceScheduleDetail(),
        phase.getStartDate(),
        phase.getEndDate()
    );
  }

  @Test
  void getLicenceScheduleRatesForTermAndDefinitionOption() {
    licenceScheduleRateService.getLicenceScheduleRatesForTermAndDefinitionOption(term, RateDefinitionOption.CUSTOM_PERIOD);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleTermAndRateDefinitionOption(
        term,
        RateDefinitionOption.CUSTOM_PERIOD
    );
  }

  @Test
  void getLicenceScheduleRatesForPhaseAndDefinitionOption() {
    licenceScheduleRateService.getLicenceScheduleRatesForPhaseAndDefinitionOption(phase, RateDefinitionOption.CUSTOM_PERIOD);

    verify(licenceScheduleRateRepository).findAllByLicenceSchedulePhaseAndRateDefinitionOption(
        phase,
        RateDefinitionOption.CUSTOM_PERIOD
    );
  }

  @Test
  void getRatesAfterDate() {
    var detail = new LicenceScheduleDetail();
    var date = LocalDate.of(2026, 1, 1);

    licenceScheduleRateService.getRatesAfterDate(detail, date);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetailAndStartDateAfter(detail, date);
  }

  @Test
  void saveLicenceScheduleRates() {
    licenceScheduleRateService.saveLicenceScheduleRates(List.of(rate));

    verify(licenceScheduleRateRepository).saveAll(List.of(rate));
  }

  @Test
  void getRateByScheduleDetailAndEventReferenceOrThrow() {
    var detail = new LicenceScheduleDetail();
    var eventReference = new EventReference();

    when(licenceScheduleRateRepository.findByLicenceScheduleDetailAndEventReference(detail, eventReference))
        .thenReturn(Optional.of(rate));

    assertThat(licenceScheduleRateService.getRateByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isEqualTo(rate);
  }

  @Test
  void getRateByScheduleDetailAndEventReferenceOrThrow_notFound() {
    var eventReference = new EventReference();
    eventReference.setId(UUID.randomUUID());

    when(licenceScheduleRateRepository.findByLicenceScheduleDetailAndEventReference(any(), any()))
        .thenReturn(Optional.empty());

    var detail =  new LicenceScheduleDetail();
    assertThatThrownBy(() -> licenceScheduleRateService.getRateByScheduleDetailAndEventReferenceOrThrow(detail, eventReference))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void deleteLicenceScheduleRate() {
    licenceScheduleRateService.deleteLicenceScheduleRate(rate);

    verify(licenceScheduleRateRepository).delete(rate);
  }

  @Test
  void getAllRatesLinkedTo_term() {
    licenceScheduleRateService.getAllRatesLinkedTo(term);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleTerm(term);
  }

  @Test
  void getAllRatesLinkedTo_phase() {
    licenceScheduleRateService.getAllRatesLinkedTo(phase);

    verify(licenceScheduleRateRepository).findAllByLicenceSchedulePhase(phase);
  }
}
