package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@Service
public class LicenceScheduleDetailService {

  private final LicenceScheduleDetailRepository licenceScheduleDetailRepository;
  private final LicenceScheduleService licenceScheduleService;

  public LicenceScheduleDetailService(
      LicenceScheduleDetailRepository licenceScheduleDetailRepository,
      LicenceScheduleService licenceScheduleService
  ) {
    this.licenceScheduleDetailRepository = licenceScheduleDetailRepository;
    this.licenceScheduleService = licenceScheduleService;
  }

  public Optional<LicenceScheduleDetail> getByLicenceSchedule(LicenceSchedule licenceSchedule) {
    return licenceScheduleDetailRepository.findByLicenceSchedule(licenceSchedule);
  }

  @Transactional
  public LicenceScheduleDetail createNewLicenceScheduleEntitiesForLicence(Licence licence) {
    var licenceSchedule = licenceScheduleService.createNewLicenceScheduleForLicence(licence);

    return createNewLicenceScheduleDetail(licenceSchedule);
  }

  @Transactional
  LicenceScheduleDetail createNewLicenceScheduleDetail(LicenceSchedule licenceSchedule) {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);

    return licenceScheduleDetailRepository.save(licenceScheduleDetail);
  }
}
