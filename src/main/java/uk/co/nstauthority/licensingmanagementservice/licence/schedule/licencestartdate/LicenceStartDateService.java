package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
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

  public LicenceStartDate getByLicenceScheduleDetailOrThrow(LicenceScheduleDetail licenceScheduleDetail) {
    return licenceStartDateRepository.findByLicenceScheduleDetail(licenceScheduleDetail)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Licence start date not found for licence schedule detail with id: " + licenceScheduleDetail.getId())
        );
  }

  @Transactional
  public LicenceStartDate saveOrUpdateLicenceStartDateFromForm(
      LicenceStartDateForm form,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var licenceStartDate = licenceStartDateRepository.findByLicenceScheduleDetail(licenceScheduleDetail)
        .orElse(new LicenceStartDate());

    licenceStartDate.setLicenceScheduleDetail(licenceScheduleDetail);
    form.getLicenceStartDate().getAsLocalDate().ifPresent(licenceStartDate::setStartDate);

    return licenceStartDateRepository.save(licenceStartDate);
  }

  @Transactional
  public LicenceStartDate saveNewLicenceStartDateFromForm(LicenceStartDateForm form, Licence licence) {
    var licenceScheduleDetail = licenceScheduleDetailService.createNewLicenceScheduleEntitiesForLicence(licence);
    return saveOrUpdateLicenceStartDateFromForm(form, licenceScheduleDetail);
  }

  public LicenceStartDateForm getLicenceStartDateForm(LicenceScheduleDetail licenceScheduleDetail) {
    var licenceStartDate = getByLicenceScheduleDetailOrThrow(licenceScheduleDetail);

    var licenceStartDateForm = new LicenceStartDateForm();
    licenceStartDateForm.getLicenceStartDate().setDate(licenceStartDate.getStartDate());
    return licenceStartDateForm;
  }
}
