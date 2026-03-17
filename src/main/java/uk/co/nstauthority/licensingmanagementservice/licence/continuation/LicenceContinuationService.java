package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class LicenceContinuationService {

  private static final String APPLICATION_REFERENCE_FORMAT = "LMS/EAA/%d/%d";

  private final ApplicationAccessService applicationAccessService;
  public static final String LICENCE_CONTINUATION_APPLICATION_DETAIL = "licence continuation application detail";
  private final LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;
  private final LicenceContinuationApplicationRepository licenceContinuationApplicationRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final Clock clock;
  private final ApplicationLetterService applicationLetterService;

  public LicenceContinuationService(
      LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository,
      LicenceContinuationApplicationRepository licenceContinuationApplicationRepository,
      LicenceScheduleDetailService licenceScheduleDetailService,
      Clock clock,
      ApplicationAccessService applicationAccessService,
      ApplicationLetterService applicationLetterService
  ) {
    this.licenceContinuationApplicationDetailRepository = licenceContinuationApplicationDetailRepository;
    this.licenceContinuationApplicationRepository = licenceContinuationApplicationRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.clock = clock;
    this.applicationAccessService = applicationAccessService;
    this.applicationLetterService = applicationLetterService;
  }

  @Transactional
  public LicenceContinuationApplicationDetail createNewLicenceContinuationApplication(
      Licence licence,
      Integer responsibleOrganisationUnitId
  ) {
    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );

    var licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());

    licenceContinuationApplicationRepository.save(licenceContinuationApplication);

    var licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
    licenceContinuationApplicationDetail.setLicenceContinuationApplication(licenceContinuationApplication);
    licenceContinuationApplicationDetail.setVersionNumber(1);
    licenceContinuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.DRAFT);
    licenceContinuationApplicationDetail.setCreatedDateTime(Instant.now(clock));
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(responsibleOrganisationUnitId);

    licenceContinuationApplicationDetailRepository.save(licenceContinuationApplicationDetail);

    return licenceContinuationApplicationDetail;
  }

  public LicenceContinuationApplicationDetail getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(
      UUID applicationId
  ) {
    var licenceContinuationApplication = licenceContinuationApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new LmsEntityNotFoundException(LICENCE_CONTINUATION_APPLICATION_DETAIL, applicationId));

    return licenceContinuationApplicationDetailRepository.findFirstByLicenceContinuationApplicationOrderByVersionNumberDesc(
            licenceContinuationApplication
        )
        .orElseThrow(() -> new LmsEntityNotFoundException(LICENCE_CONTINUATION_APPLICATION_DETAIL, applicationId));
  }

  public LicenceContinuationApplication getApplicationByIdOrThrow(UUID applicationId) {
    return licenceContinuationApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new LmsEntityNotFoundException("licence continuation application", applicationId));
  }

  public LicenceContinuationApplicationDetail getDetailByIdOrThrow(UUID detailId) {
    return licenceContinuationApplicationDetailRepository.findById(detailId)
        .orElseThrow(() -> new LmsEntityNotFoundException(LICENCE_CONTINUATION_APPLICATION_DETAIL, detailId));
  }

  public List<LicenceContinuationApplicationDetail> getAllContinuationApplicationDetailsByStatus(
      LicenceContinuationApplicationStatus status
  ) {
    return licenceContinuationApplicationDetailRepository.findAllByStatus(status);
  }

  public List<LicenceContinuationApplicationDetail> getAllContinuationApplicationDetailsByStatuses(
      Set<LicenceContinuationApplicationStatus> statuses
  ) {
    return licenceContinuationApplicationDetailRepository.findAllByStatusIn(statuses);
  }

  public LicenceScheduleDetail getScheduleDetailFromApplicationDetail(
      LicenceContinuationApplicationDetail detail
  ) {
    var app = detail.getLicenceContinuationApplication();
    if (app.getSubmittedLicenceScheduleDetail() != null) {
      return app.getSubmittedLicenceScheduleDetail();
    }
    var licence = app.getLicenceSchedule().getLicence();
    return licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(licence, LicenceScheduleDetailStatus.ACTIVE);
  }

  public Licence getLicenceFromContinuationApplicationDetail(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    return getScheduleDetailFromApplicationDetail(licenceContinuationApplicationDetail).getLicenceSchedule().getLicence();
  }

  @Transactional
  public LicenceContinuationApplication submitApplication(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      ServiceUserDetail user
  ) {
    var appReference = generateApplicationReference();

    LicenceContinuationApplication licenceContinuationApplication
        = licenceContinuationApplicationDetail.getLicenceContinuationApplication();
    licenceContinuationApplication.setApplicationReference(appReference);

    var activeDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licenceContinuationApplication.getLicenceSchedule().getLicence(),
        LicenceScheduleDetailStatus.ACTIVE);
    licenceContinuationApplication.setSubmittedLicenceScheduleDetail(activeDetail);

    licenceContinuationApplicationRepository.save(licenceContinuationApplication);

    licenceContinuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.SUBMITTED);
    licenceContinuationApplicationDetail.setSubmittedDatetime(Instant.now(clock));
    licenceContinuationApplicationDetail.setSubmittedByWuaId(user.wuaId());

    licenceContinuationApplicationDetailRepository.save(licenceContinuationApplicationDetail);

    return licenceContinuationApplication;
  }

  @Transactional
  public void confirmContinuationChangeStatus(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    applicationLetterService.createDocumentInstance(
        licenceContinuationApplicationDetail.getLicenceContinuationApplication()
    );
    licenceContinuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.ISSUE_DECISION);
    licenceContinuationApplicationDetailRepository.save(licenceContinuationApplicationDetail);
  }

  private String generateApplicationReference() {
    var currentYear = LocalDate.now(clock).getYear();
    var submissionsForYear = getSubmissionsForYear(currentYear);

    return String.format(APPLICATION_REFERENCE_FORMAT, currentYear, submissionsForYear + 1);
  }

  private int getSubmissionsForYear(int currentYear) {
    var startOfYear = DateUtil.getStartOfYear(clock, currentYear);
    var endOfYear = DateUtil.getEndOfYear(clock, currentYear);

    return licenceContinuationApplicationDetailRepository.countByVersionNumberAndStatusAndSubmittedDatetimeBetween(
        1,
        LicenceContinuationApplicationStatus.SUBMITTED,
        startOfYear,
        endOfYear
    );
  }

  public boolean userCanSubmitApplication(LicenceContinuationApplicationDetail applicationDetail, ServiceUserDetail user) {
    return applicationAccessService.userIsSubmitterForOrganisationUnit(
        applicationDetail.getResponsibleOrganisationUnitId(),
        user.wuaId()
    );
  }
}
