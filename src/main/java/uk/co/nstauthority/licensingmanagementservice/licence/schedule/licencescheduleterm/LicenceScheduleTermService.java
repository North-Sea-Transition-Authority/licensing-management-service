package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleTermService {

  private final LicenceScheduleTermRepository licenceScheduleTermRepository;

  public LicenceScheduleTermService(LicenceScheduleTermRepository licenceScheduleTermRepository) {
    this.licenceScheduleTermRepository = licenceScheduleTermRepository;
  }

  public List<LicenceScheduleTerm> getTermsByLicenceScheduleDetail(LicenceScheduleDetail scheduleDetail) {
    return licenceScheduleTermRepository.findByLicenceScheduleDetail(scheduleDetail);
  }

  public void saveTerms(List<LicenceScheduleTerm> licenceScheduleTerms) {
    licenceScheduleTermRepository.saveAll(licenceScheduleTerms);
  }
}
