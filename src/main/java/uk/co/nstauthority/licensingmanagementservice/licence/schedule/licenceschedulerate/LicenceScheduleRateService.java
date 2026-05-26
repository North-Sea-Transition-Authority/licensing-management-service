package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
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

  public List<LicenceScheduleRate> getLicenceScheduleRates(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesAttachedToTerm(LicenceScheduleTerm licenceScheduleTerm) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(
        licenceScheduleTerm,
        RateDefinitionOption.TERM
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesByTerm(
      LicenceScheduleTerm licenceScheduleTerm
  ) {
    var ratesByTerm = getActiveLicenceScheduleRatesAttachedToTerm(licenceScheduleTerm);

    if (!ratesByTerm.isEmpty()) {
      return ratesByTerm;
    }

    return licenceScheduleRateRepository.findAllByLicenceScheduleDetailAndStartDateBetween(
        licenceScheduleTerm.getLicenceScheduleDetail(),
        licenceScheduleTerm.getStartDate(),
        licenceScheduleTerm.getEndDate()
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesByPhase(
      LicenceSchedulePhase licenceSchedulePhase,
      PhaseType firstPhaseType
  ) {
    if (licenceSchedulePhase.getPhaseType().equals(firstPhaseType)) {
      var ratesByTerm = licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(
          licenceSchedulePhase.getLicenceScheduleTerm(),
          RateDefinitionOption.TERM
      );

      if (!ratesByTerm.isEmpty()) {
        return List.of();
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

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesForTermAndDefinitionOption(
      LicenceScheduleTerm licenceScheduleTerm,
      RateDefinitionOption rateDefinitionOption
  ) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleTermAndRateDefinitionOption(
        licenceScheduleTerm,
        rateDefinitionOption
    );
  }

  public List<LicenceScheduleRate> getActiveLicenceScheduleRatesForPhaseAndDefinitionOption(
      LicenceSchedulePhase licenceSchedulePhase,
      RateDefinitionOption rateDefinitionOption
  ) {
    return licenceScheduleRateRepository.findAllByLicenceSchedulePhaseAndRateDefinitionOption(
        licenceSchedulePhase,
        rateDefinitionOption
    );
  }

  public List<LicenceScheduleRate> getActiveRatesAfterDate(
      LicenceScheduleDetail licenceScheduleDetail,
      LocalDate date
  ) {
    return licenceScheduleRateRepository.findAllByLicenceScheduleDetailAndStartDateAfter(licenceScheduleDetail, date);
  }

  @Transactional
  public void saveLicenceScheduleRates(List<LicenceScheduleRate> licenceScheduleRates) {
    licenceScheduleRateRepository.saveAll(licenceScheduleRates);
  }

  public LicenceScheduleRate getRateByScheduleDetailAndEventReferenceOrThrow(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    return licenceScheduleRateRepository.findByLicenceScheduleDetailAndEventReference(scheduleDetail, eventReference)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceScheduleRate", eventReference.getId()));
  }

  @Transactional
  public void deleteLicenceScheduleRate(LicenceScheduleRate licenceScheduleRate) {
    licenceScheduleRateRepository.delete(licenceScheduleRate);
  }
}
