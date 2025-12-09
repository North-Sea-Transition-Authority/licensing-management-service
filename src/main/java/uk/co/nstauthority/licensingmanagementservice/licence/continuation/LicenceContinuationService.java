package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@Service
public class LicenceContinuationService {

  private final LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;
  private final LicenceContinuationApplicationRepository licenceContinuationApplicationRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final Clock clock;

  public LicenceContinuationService(LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository,
                                    LicenceContinuationApplicationRepository licenceContinuationApplicationRepository,
                                    LicenceScheduleDetailService licenceScheduleDetailService, Clock clock) {
    this.licenceContinuationApplicationDetailRepository = licenceContinuationApplicationDetailRepository;
    this.licenceContinuationApplicationRepository = licenceContinuationApplicationRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.clock = clock;
  }

  @Transactional
  public LicenceContinuationApplicationDetail createNewLicenceContinuationApplication(Licence licence) {
    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );

    var licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setLicenceScheduleDetail(licenceScheduleDetail);

    licenceContinuationApplicationRepository.save(licenceContinuationApplication);

    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setLicenceContinuationApplication(licenceContinuationApplication);
    licenceContinuationApplicationDetail.setVersionNumber(1);
    licenceContinuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.DRAFT);
    licenceContinuationApplicationDetail.setCreatedDateTime(Instant.now(clock));

    licenceContinuationApplicationDetailRepository.save(licenceContinuationApplicationDetail);

    return licenceContinuationApplicationDetail;
  }

  public LicenceContinuationApplicationDetail getDetailByIdOrThrow(UUID detailId) {
    return licenceContinuationApplicationDetailRepository.findById(detailId)
        .orElseThrow(() -> new LmsEntityNotFoundException("licence continuation application detail", detailId));
  }
}
