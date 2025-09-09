package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceSchedulePhaseService {

  private final LicenceSchedulePhaseRepository licenceSchedulePhaseRepository;

  public LicenceSchedulePhaseService(LicenceSchedulePhaseRepository licenceSchedulePhaseRepository) {
    this.licenceSchedulePhaseRepository = licenceSchedulePhaseRepository;
  }

  public List<LicenceSchedulePhase> getPhasesByLicenceScheduleDetail(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceSchedulePhaseRepository.findByLicenceScheduleDetail(licenceScheduleDetail);
  }
}
