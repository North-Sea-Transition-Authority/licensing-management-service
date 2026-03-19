package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney.LicenseeInformationForm;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Service
public class ScheduleWorkProgrammeApplicationService {
  private static final String APPLICATION_REFERENCE_FORMAT = "LMS/EAA/%d/%d";

  private final ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;
  private final ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final Clock clock;
  private final ApplicationAccessService applicationAccessService;

  public ScheduleWorkProgrammeApplicationService(
      ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository,
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository,
      LicenceScheduleDetailService licenceScheduleDetailService,
      Clock clock,
      ApplicationAccessService applicationAccessService) {
    this.scheduleWorkProgrammeApplicationRepository = scheduleWorkProgrammeApplicationRepository;
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.clock = clock;
    this.applicationAccessService = applicationAccessService;
  }

  @Transactional
  public ScheduleWorkProgrammeApplicationDetail createNewScheduleWorkProgrammeApplicationForLicence(
      @NotNull Licence licence,
      LicenseeInformationForm licenseeInformationForm) {
    var scheduleWorkProgrammeApplication = createScheduleWorkProgrammeApplication(licence);
    var responsibleOrganisationUnitId = licenseeInformationForm.getResponsibleOrganisationUnitId();
    var scheduleWorkProgrammeApplicationDetail = createScheduleWorkProgrammeApplicationDetail(
        scheduleWorkProgrammeApplication,
        licenseeInformationForm.getAllLicenseesPermissionConfirmed(),
        responsibleOrganisationUnitId
    );

    scheduleWorkProgrammeApplicationRepository.save(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetailRepository.save(scheduleWorkProgrammeApplicationDetail);

    return scheduleWorkProgrammeApplicationDetail;
  }

  public LicenceScheduleDetail getScheduleDetailFromApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail detail
  ) {
    var app = detail.getScheduleWorkProgrammeApplication();
    if (app.getSubmittedLicenceScheduleDetail() != null) {
      return app.getSubmittedLicenceScheduleDetail();
    }
    return licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        app.getLicenceSchedule().getLicence(), LicenceScheduleDetailStatus.ACTIVE);
  }

  public Licence getLicenceFromScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return getScheduleDetailFromApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        .getLicenceSchedule().getLicence();
  }

  private ScheduleWorkProgrammeApplication createScheduleWorkProgrammeApplication(Licence licence) {
    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    return scheduleWorkProgrammeApplication;
  }

  private ScheduleWorkProgrammeApplicationDetail createScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication,
      Boolean allLicenseesPermissionConfirmed,
      Integer responsibleOrganisationUnitId) {
    var scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication);
    scheduleWorkProgrammeApplicationDetail.setVersionNumber(1);
    scheduleWorkProgrammeApplicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT);
    scheduleWorkProgrammeApplicationDetail.setCreatedDatetime(Instant.now(clock));
    scheduleWorkProgrammeApplicationDetail.setAllLicenseesPermissionConfirmed(allLicenseesPermissionConfirmed);
    scheduleWorkProgrammeApplicationDetail.setResponsibleOrganisationUnitId(responsibleOrganisationUnitId);

    return scheduleWorkProgrammeApplicationDetail;
  }

  @Transactional
  public void deleteScheduleWorkProgrammeApplication(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    scheduleWorkProgrammeApplicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.DELETED);
    scheduleWorkProgrammeApplicationDetailRepository.save(scheduleWorkProgrammeApplicationDetail);
  }

  public ScheduleWorkProgrammeApplication getApplicationByIdOrThrow(UUID applicationId) {
    return scheduleWorkProgrammeApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new LmsEntityNotFoundException("schedule work programme application detail", applicationId));
  }

  public ScheduleWorkProgrammeApplicationDetail getDetailByIdOrThrow(UUID detailId) {
    return scheduleWorkProgrammeApplicationDetailRepository.findById(detailId).orElseThrow(
        () -> new LmsEntityNotFoundException("schedule work programme application detail", detailId));
  }

  public ScheduleWorkProgrammeApplicationDetail getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(
      ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication
  ) {
    return scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(scheduleWorkProgrammeApplication)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Schedule Work Programme Application Details not found",
            scheduleWorkProgrammeApplication.getId()
        ));
  }

  public ScheduleWorkProgrammeApplicationDetail getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(UUID applicationId) {
    var scheduleWorkProgrammeApplication = scheduleWorkProgrammeApplicationRepository.findById(applicationId)
        .orElseThrow(() -> new LmsEntityNotFoundException("schedule work programme application", applicationId));

    return scheduleWorkProgrammeApplicationDetailRepository.getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(
        scheduleWorkProgrammeApplication
        )
        .orElseThrow(() -> new LmsEntityNotFoundException("schedule work programme application detail", applicationId));
  }

  public ScheduleWorkProgrammeApplication getScheduleWorkProgrammeApplicationById(UUID scheduleWorkProgrammeApplicationId) {
    return scheduleWorkProgrammeApplicationRepository.findById(scheduleWorkProgrammeApplicationId)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Schedule Work Programme Application not found",
            scheduleWorkProgrammeApplicationId
        ));
  }

  public List<ScheduleWorkProgrammeApplicationDetail> getAllScheduleWorkProgrammeApplicationDetailsByStatuses(
      Set<ScheduleWorkProgrammeApplicationStatus> statuses
  ) {
    return scheduleWorkProgrammeApplicationDetailRepository.findAllByStatusIn(statuses);
  }

  @Transactional
  public ScheduleWorkProgrammeApplication submitApplication(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user) {
    var appReference = generateApplicationReference();

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication
        = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setApplicationReference(appReference);

    var activeDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        scheduleWorkProgrammeApplication.getLicenceSchedule().getLicence(),
        LicenceScheduleDetailStatus.ACTIVE);
    scheduleWorkProgrammeApplication.setSubmittedLicenceScheduleDetail(activeDetail);

    scheduleWorkProgrammeApplicationRepository.save(scheduleWorkProgrammeApplication);

    scheduleWorkProgrammeApplicationDetail.setStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail.setSubmittedDatetime(Instant.now(clock));
    scheduleWorkProgrammeApplicationDetail.setSubmittedByWuaId(user.wuaId());

    scheduleWorkProgrammeApplicationDetailRepository.save(scheduleWorkProgrammeApplicationDetail);

    return scheduleWorkProgrammeApplication;
  }

  private String generateApplicationReference() {
    var currentYear = LocalDate.now(clock).getYear();
    var submissionsForYear = getSubmissionsForYear(currentYear);

    return String.format(APPLICATION_REFERENCE_FORMAT, currentYear, submissionsForYear + 1);
  }

  private int getSubmissionsForYear(int currentYear) {
    var startOfYear = DateUtil.getStartOfYear(clock, currentYear);
    var endOfYear = DateUtil.getEndOfYear(clock, currentYear);

    return scheduleWorkProgrammeApplicationDetailRepository.countByVersionNumberAndSubmittedDatetimeBetween(
        1,
        startOfYear,
        endOfYear
    );
  }

  public boolean userCanSubmitApplication(ScheduleWorkProgrammeApplicationDetail applicationDetail, ServiceUserDetail user) {
    return applicationAccessService.userIsSubmitterForOrganisationUnit(
        applicationDetail.getResponsibleOrganisationUnitId(),
        user.wuaId()
    );
  }
}