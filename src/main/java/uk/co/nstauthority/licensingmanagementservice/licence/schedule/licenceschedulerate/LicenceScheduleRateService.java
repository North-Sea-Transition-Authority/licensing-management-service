package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class LicenceScheduleRateService {

  private final LicenceScheduleRateRepository licenceScheduleRateRepository;

  public LicenceScheduleRateService(LicenceScheduleRateRepository licenceScheduleRateRepository) {
    this.licenceScheduleRateRepository = licenceScheduleRateRepository;
  }

  public LicenceScheduleRate getRateByIdOrThrow(UUID id) {
    return licenceScheduleRateRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceScheduleRate not found", id));
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesAttachedToTerm(LicenceScheduleTerm licenceScheduleTerm) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOptionAndStatus(
        licenceScheduleTerm,
        RateDefinitionOption.TERM,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesByTerm(
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var ratesByTerm = getActiveLicenceScheduleRatesAttachedToTerm(licenceScheduleTerm);

    if (!ratesByTerm.isEmpty()) {
      return ratesByTerm;
    }

    return licenceScheduleRateRepository.findAllByLicenceScheduleDetailAndStartDateBetweenAndStatus(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate(),
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesByPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType
  ) {
    if (licenceSchedulePhase.getPhaseType().equals(firstPhaseType)) {
      var ratesByTerm = licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOptionAndStatus(
          licenceSchedulePhase.getLicenceScheduleTerm(),
          RateDefinitionOption.TERM,
          LicenceScheduleEventStatus.ACTIVE
      );

      if (!ratesByTerm.isEmpty()) {
        return List.of();
      }
    }

    var ratesByPhase = licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOptionAndStatus(
        licenceSchedulePhase,
        RateDefinitionOption.PHASE,
        LicenceScheduleEventStatus.ACTIVE
    );

    if (!ratesByPhase.isEmpty()) {
      return ratesByPhase;
    }

    return licenceScheduleRateRepository.findAllByLicenceScheduleDetailAndStartDateBetweenAndStatus(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate(),
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesForTermAndDefinitionOption(
      LicenceScheduleTerm licenceScheduleTerm,
      RateDefinitionOption rateDefinitionOption
  ) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOptionAndStatus(
        licenceScheduleTerm,
        rateDefinitionOption,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesForPhaseAndDefinitionOption(
      LicenceSchedulePhase licenceSchedulePhase,
      RateDefinitionOption rateDefinitionOption
  ) {
    return licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOptionAndStatus(
        licenceSchedulePhase,
        rateDefinitionOption,
        LicenceScheduleEventStatus.ACTIVE
    );
  }

  @Transactional
  public void saveLicenceScheduleRates(List<LicenceScheduleRate> licenceScheduleRates) {
    licenceScheduleRateRepository.saveAll(licenceScheduleRates);
  }

  @Transactional
  public void deleteLicenceScheduleRate(LicenceScheduleRate licenceScheduleRate) {
    licenceScheduleRate.setStatus(LicenceScheduleEventStatus.DELETED);
    licenceScheduleRateRepository.save(licenceScheduleRate);
  }
}
