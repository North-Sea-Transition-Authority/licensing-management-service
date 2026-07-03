package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent.OtherScheduleEventService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityService;

@Service
public class LicenceSchedulePhaseService {

  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;
  private final LicenceScheduleRateService licenceScheduleRateService;
  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final OtherScheduleEventService otherScheduleEventService;

  public LicenceSchedulePhaseService(
      LicenceSchedulePhaseRepository licenceSchedulePhaseRepository,
      LicenceScheduleRateService licenceScheduleRateService,
      WorkProgrammeActivityService workProgrammeActivityService,
      OtherScheduleEventService otherScheduleEventService
  ) {
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.otherScheduleEventService = otherScheduleEventService;
  }

  public LicenceSchedulePhase getPhaseByIdOrThrow(UUID id) {
    return licenceSchedulePhaseRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceSchedulePhase not found", id.toString()));
  }

  public List<LicenceSchedulePhase> getActivePhasesByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceSchedulePhaseRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Transactional
  public void saveLicenceSchedulePhases(List<LicenceSchedulePhase> licenceSchedulePhase) {
    licenceSchedulePhaseRepository.saveAll(licenceSchedulePhase);
  }

  public List<LicenceSchedulePhase> getPhasesByTerm(LicenceScheduleTerm licenceScheduleTerm) {
    return licenceSchedulePhaseRepository.findAllByLicenceScheduleTerm(licenceScheduleTerm);
  }

  boolean canDeletePhase(LicenceSchedulePhase licenceSchedulePhase) {
    if (!licenceScheduleRateService.getAllRatesLinkedTo(licenceSchedulePhase).isEmpty()) {
      return false;
    }
    if (!workProgrammeActivityService.getAllActivitiesLinkedTo(licenceSchedulePhase).isEmpty()) {
      return false;
    }
    return otherScheduleEventService.getAllEventsLinkedTo(licenceSchedulePhase).isEmpty();
  }

  @Transactional
  void deletePhase(LicenceSchedulePhase licenceSchedulePhase) {
    if (canDeletePhase(licenceSchedulePhase)) {
      licenceSchedulePhaseRepository.delete(licenceSchedulePhase);
    } else {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Cannot delete phase id: %s as it is referenced by other schedule events".formatted(licenceSchedulePhase.getId())
      );
    }
  }
}
