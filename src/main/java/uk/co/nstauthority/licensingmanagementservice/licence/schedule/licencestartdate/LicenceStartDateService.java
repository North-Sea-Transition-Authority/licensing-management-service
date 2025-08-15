package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;

@Service
public class LicenceStartDateService {

  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceStartDateRepository licenceStartDateRepository;

  public LicenceStartDateService(
      LicenceStartDateRepository licenceStartDateRepository,
      LicenceScheduleDetailService licenceScheduleDetailService
  ) {
    this.licenceStartDateRepository = licenceStartDateRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  @Transactional
  public LicenceStartDate saveNewLicenceStartDateFromForm(LicenceStartDateForm form, Licence licence) {
    var licenceScheduleDetail = licenceScheduleDetailService.createNewLicenceScheduleEntitiesForLicence(licence);

    var licenceStartDate = new LicenceStartDate();
    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);
    form.getLicenceStartDate().getAsLocalDate().ifPresent(licenceStartDate::setStartDate);

    return licenceStartDateRepository.save(licenceStartDate);
  }
}
