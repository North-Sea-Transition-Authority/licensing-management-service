package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleTermService {

  private final LicenceScheduleTermRepository licenceScheduleTermRepository;

  public LicenceScheduleTermService(LicenceScheduleTermRepository licenceScheduleTermRepository) {
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
  }

  public List<LicenceScheduleTerm> getActiveTermsByLicenceScheduleDetail(LicenceScheduleDetail scheduleDetail) {
    return licenceScheduleTermRepository.findByLicenceScheduleDetailAndStatus(scheduleDetail, LicenceScheduleEventStatus.ACTIVE);
  }

  @Transactional
  public Iterable<LicenceScheduleTerm> saveTerms(List<LicenceScheduleTerm> licenceScheduleTerms) {
    return licenceScheduleTermRepository.saveAll(licenceScheduleTerms);
  }

  LicenceScheduleTerm getTermByIdOrThrow(UUID id) {
    return licenceScheduleTermRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException("LicenceScheduleTerm not found", id.toString()));
  }

  @Transactional
  void deleteTerm(LicenceScheduleTerm licenceScheduleTerm) {
    licenceScheduleTerm.setStatus(LicenceScheduleEventStatus.DELETED);
    licenceScheduleTermRepository.save(licenceScheduleTerm);
  }
}
