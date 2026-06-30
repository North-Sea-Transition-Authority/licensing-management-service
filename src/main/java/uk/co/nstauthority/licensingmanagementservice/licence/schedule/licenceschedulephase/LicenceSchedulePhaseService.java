package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventreference.EventReference;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;

@Service
public class LicenceSchedulePhaseService {

  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  public LicenceSchedulePhaseService(LicenceSchedulePhaseRepository licenceSchedulePhaseRepository) {
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
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

  public LicenceSchedulePhase getPhaseByScheduleDetailAndEventReferenceOrThrow(
      LicenceScheduleDetail scheduleDetail,
      EventReference eventReference
  ) {
    return licenceSchedulePhaseRepository.findByLicenceScheduleDetailAndEventReference(scheduleDetail, eventReference)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceSchedulePhase", eventReference.getId()));
  }

  @Transactional
  void deletePhase(LicenceSchedulePhase licenceSchedulePhase) {
    licenceSchedulePhaseRepository.delete(licenceSchedulePhase);
  }
}
