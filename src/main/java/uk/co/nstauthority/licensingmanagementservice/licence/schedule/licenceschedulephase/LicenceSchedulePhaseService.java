package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

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

  public List<LicenceSchedulePhase> getPhasesByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceSchedulePhaseRepository.findByLicenceScheduleDetail(licenceScheduleDetail);
  }

  @Transactional
  public void saveLicenceSchedulePhases(List<LicenceSchedulePhase> licenceSchedulePhase) {
    licenceSchedulePhaseRepository.saveAll(licenceSchedulePhase);
  }
}
