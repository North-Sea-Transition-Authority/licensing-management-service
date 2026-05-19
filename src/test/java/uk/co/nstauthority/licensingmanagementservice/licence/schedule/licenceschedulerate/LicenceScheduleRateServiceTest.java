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
  void getActiveLicenceScheduleRatesAttachedToTerm() {
    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getActiveLicenceScheduleRatesAttachedToTerm(term)).isEqualTo(List.of(rate));
  }

  @Test
  void getActiveLicenceScheduleRatesByTerm_forTerm() {
    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term)).isEqualTo(List.of(rate));
  }

  @Test
  void getActiveLicenceScheduleRatesByTerm_termDateRange() {
    term.setLicenceScheduleDetail(new LicenceScheduleDetail());
    term.setStartDate(LocalDate.of(2025, 1, 1));
    term.setEndDate(LocalDate.of(2025, 12, 31));

    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of());

    licenceScheduleRateService.getActiveLicenceScheduleRatesByTerm(term);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetailAndStartDateBetween(
        term.getLicenceScheduleDetail(),
        term.getStartDate(),
        term.getEndDate()
    );
  }

  @Test
  void getActiveLicenceScheduleRatesByPhase_forTerm() {
    phase.setPhaseType(PhaseType.PHASE_A);
    phase.setLicenceScheduleTerm(term);

    when(licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(term, RateDefinitionOption.TERM))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).isEqualTo(List.of());
  }

  @Test
  void getActiveLicenceScheduleRatesByPhase_forPhase() {
    phase.setPhaseType(PhaseType.PHASE_B);

    when(licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(phase, RateDefinitionOption.PHASE))
        .thenReturn(List.of(rate));

    assertThat(licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A)).isEqualTo(List.of(rate));
  }

  @Test
  void getActiveLicenceScheduleRatesByPhase_phaseDateRange() {
    phase.setLicenceScheduleDetail(new LicenceScheduleDetail());
    phase.setStartDate(LocalDate.of(2025, 1, 1));
    phase.setEndDate(LocalDate.of(2025, 12, 31));
    phase.setPhaseType(PhaseType.PHASE_B);

    when(licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(phase, RateDefinitionOption.PHASE))
        .thenReturn(List.of());

    licenceScheduleRateService.getActiveLicenceScheduleRatesByPhase(phase, PhaseType.PHASE_A);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetailAndStartDateBetween(
        phase.getLicenceScheduleDetail(),
        phase.getStartDate(),
        phase.getEndDate()
    );
  }

  @Test
  void getActiveLicenceScheduleRatesForTermAndDefinitionOption() {
    licenceScheduleRateService.getActiveLicenceScheduleRatesForTermAndDefinitionOption(term, RateDefinitionOption.CUSTOM_PERIOD);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleTermAndRateDefinitionOption(
        term,
        RateDefinitionOption.CUSTOM_PERIOD
    );
  }

  @Test
  void getActiveLicenceScheduleRatesForPhaseAndDefinitionOption() {
    licenceScheduleRateService.getActiveLicenceScheduleRatesForPhaseAndDefinitionOption(phase, RateDefinitionOption.CUSTOM_PERIOD);

    verify(licenceScheduleRateRepository).findAllByLicenceSchedulePhaseAndRateDefinitionOption(
        phase,
        RateDefinitionOption.CUSTOM_PERIOD
    );
  }

  @Test
  void getActiveRatesAfterDate() {
    var detail = new LicenceScheduleDetail();
    var date = LocalDate.of(2026, 1, 1);

    licenceScheduleRateService.getActiveRatesAfterDate(detail, date);

    verify(licenceScheduleRateRepository).findAllByLicenceScheduleDetailAndStartDateAfter(detail, date);
  }

  @Test
  void saveLicenceScheduleRates() {
    licenceScheduleRateService.saveLicenceScheduleRates(List.of(rate));

    verify(licenceScheduleRateRepository).saveAll(List.of(rate));
  }

  @Test
  void deleteLicenceScheduleRate() {
    licenceScheduleRateService.deleteLicenceScheduleRate(rate);

    verify(licenceScheduleRateRepository).delete(rate);
  }
}
