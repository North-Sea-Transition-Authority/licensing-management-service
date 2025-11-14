package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
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

  public LicenceScheduleDetail getScheduleDetailByLicenceAndStatusOrThrow(Licence licence, LicenceScheduleDetailStatus status) {
    return licenceScheduleDetailRepository.findByLicenceSchedule_LicenceAndStatus(licence, status)
        .orElseThrow(() -> new LmsEntityNotFoundException("licence schedule detail", licence.getId()));
  }

  //TODO remove in place of draft schedules for the user
  public List<LicenceScheduleDetail> getAllDraftLicenceScheduleDetails(ServiceUserDetail serviceUserDetail) {
    return licenceScheduleDetailRepository.findAllByStatus(LicenceScheduleDetailStatus.DRAFT);
  }

  @Transactional
  public LicenceScheduleDetail createNewLicenceScheduleEntitiesForLicence(Licence licence) {
    var licenceSchedule = licenceScheduleService.getOrCreateNewLicenceScheduleForLicence(licence);

    return createNewDraftLicenceScheduleDetail(licenceSchedule);
  }

  @Transactional
  LicenceScheduleDetail createNewDraftLicenceScheduleDetail(LicenceSchedule licenceSchedule) {
    var licenceScheduleDetail = new LicenceScheduleDetail();
    licenceScheduleDetail.setLicenceSchedule(licenceSchedule);
    licenceScheduleDetail.setStatus(LicenceScheduleDetailStatus.DRAFT);
    licenceScheduleDetail.setCreatedInstant(Instant.now());

    return licenceScheduleDetailRepository.save(licenceScheduleDetail);
  }

  @Transactional
  public void saveLicenceScheduleDetails(List<LicenceScheduleDetail> licenceScheduleDetails) {
    licenceScheduleDetailRepository.saveAll(licenceScheduleDetails);
  }

  public boolean draftScheduleExistsForLicence(Licence licence) {
    return licenceScheduleDetailRepository.existsByLicenceSchedule_LicenceAndStatus(licence, LicenceScheduleDetailStatus.DRAFT);
  }
}
