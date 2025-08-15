package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

import jakarta.transaction.Transactional;
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

  @Transactional
  public void saveTermFromForm(
      LicenceScheduleTermForm licenceScheduleTermForm,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var licenceScheduleTerm = new LicenceScheduleTerm();
    licenceScheduleTerm.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleTerm.setTermType(licenceScheduleTermForm.getTermType());
    licenceScheduleTerm.setTermDuration(licenceScheduleTermForm.getTermDuration().toThreeFieldDuration());
    licenceScheduleTermRepository.save(licenceScheduleTerm);

    //TODO: LMS1-145 call schedule date calculation here
  }
}
