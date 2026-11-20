package uk.co.nstauthority.licensingmanagementservice.licence.schedule.areaevent;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Service
public class LicenceScheduleAreaEventService {

  private final LicenceScheduleAreaEventRepository licenceScheduleAreaEventRepository;

  public LicenceScheduleAreaEventService(LicenceScheduleAreaEventRepository licenceScheduleAreaEventRepository) {
    this.licenceScheduleAreaEventRepository = licenceScheduleAreaEventRepository;
  }

  public List<LicenceScheduleAreaEvent> getAreaEvents(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceScheduleAreaEventRepository.findAllByLicenceScheduleDetail(licenceScheduleDetail);
  }
}
