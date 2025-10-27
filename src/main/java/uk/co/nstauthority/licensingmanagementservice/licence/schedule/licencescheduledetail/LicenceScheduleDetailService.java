package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
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

  public LicenceScheduleDetail getByIdOrThrow(UUID id) {
    return licenceScheduleDetailRepository.findById(id)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licence schedule detail not found for id: %s".formatted(id.toString()))
        );
  }

  public LicenceScheduleDetail getScheduleDetailByLicenceOrThrow(Licence licence) {
    return licenceScheduleDetailRepository.findByLicenceSchedule_Licence(licence)
        .orElseThrow(() -> new LmsEntityNotFoundException("licence schedule detail", licence.getId()));
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

  @Transactional
  public void saveLicenceScheduleDetails(List<LicenceScheduleDetail> licenceScheduleDetails) {
    licenceScheduleDetailRepository.saveAll(licenceScheduleDetails);
  }
}
