package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class LicenceScheduleRateService {

  private final LicenceScheduleRateRepository licenceScheduleRateRepository;

  public LicenceScheduleRateService(LicenceScheduleRateRepository licenceScheduleRateRepository) {
    this.licenceScheduleRateRepository = licenceScheduleRateRepository;
  }

  public List<LicenceScheduleRate> getLicenceScheduleRatesByTerm(
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var ratesByTerm = licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(
        licenceScheduleTerm,
        RateDefinitionOption.TERM
    );

    if (!ratesByTerm.isEmpty()) {
      return ratesByTerm;
    }

    return licenceScheduleRateRepository.findAllByLicenceScheduleDetailAndStartDateBetween(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<LicenceScheduleRate> getLicenceScheduleRatesByPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType
  ) {
    if (licenceSchedulePhase.getPhaseType().equals(firstPhaseType)) {
      var ratesByTerm = licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(
          licenceSchedulePhase.getLicenceScheduleTerm(),
          RateDefinitionOption.TERM
      );

      if (!ratesByTerm.isEmpty()) {
        return ratesByTerm;
      }
    }

    var ratesByPhase = licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(
        licenceSchedulePhase,
        RateDefinitionOption.PHASE
    );

    if (!ratesByPhase.isEmpty()) {
      return ratesByPhase;
    }

    return licenceScheduleRateRepository.findAllByLicenceScheduleDetailAndStartDateBetween(
        licenceSchedulePhase.getLicenceScheduleDetail(),
        licenceSchedulePhase.getStartDate(),
        licenceSchedulePhase.getEndDate()
    );
  }

  public List<LicenceScheduleRate> getLicenceScheduleRatesForTermAndDefinitionOption(
      LicenceScheduleTerm licenceScheduleTerm,
      RateDefinitionOption rateDefinitionOption
  ) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(
        licenceScheduleTerm,
        rateDefinitionOption
    );
  }

  public List<LicenceScheduleRate> getLicenceScheduleRatesForPhaseAndDefinitionOption(
      LicenceSchedulePhase licenceSchedulePhase,
      RateDefinitionOption rateDefinitionOption
  ) {
    return licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(
        licenceSchedulePhase,
        rateDefinitionOption
    );
  }

  @Transactional
  public void saveLicenceScheduleRates(List<LicenceScheduleRate> licenceScheduleRates) {
    licenceScheduleRateRepository.saveAll(licenceScheduleRates);
  }
}
