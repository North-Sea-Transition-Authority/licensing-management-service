package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
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

  public ScheduleWorkProgrammeApplicationService(
      ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository,
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository,
      LicenceScheduleDetailService licenceScheduleDetailService,
      Clock clock
  ) {
    this.scheduleWorkProgrammeApplicationRepository = scheduleWorkProgrammeApplicationRepository;
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.clock = clock;
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

  public Licence getLicenceFromScheduleWorkProgrammeApplicationDetail(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail
  ) {
    return scheduleWorkProgrammeApplicationDetail
        .getScheduleWorkProgrammeApplication()
        .getLicenceScheduleDetail()
        .getLicenceSchedule()
        .getLicence();
  }

  private ScheduleWorkProgrammeApplication createScheduleWorkProgrammeApplication(Licence licence) {
    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );

    var scheduleWorkProgrammeApplication = new ScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setLicenceScheduleDetail(licenceScheduleDetail);
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

  public ScheduleWorkProgrammeApplication getScheduleWorkProgrammeApplicationById(UUID scheduleWorkProgrammeApplicationId) {
    return scheduleWorkProgrammeApplicationRepository.findById(scheduleWorkProgrammeApplicationId)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Schedule Work Programme Application not found",
            scheduleWorkProgrammeApplicationId
        ));
  }

  public List<ScheduleWorkProgrammeApplicationDetail> getAllScheduleWorkProgrammeApplicationDetailsByStatus(
      ScheduleWorkProgrammeApplicationStatus status
  ) {
    return scheduleWorkProgrammeApplicationDetailRepository.findAllByStatus(status);
  }

  @Transactional
  public ScheduleWorkProgrammeApplication submitApplication(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      ServiceUserDetail user) {
    var appReference = generateApplicationReference();

    ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication
        = scheduleWorkProgrammeApplicationDetail.getScheduleWorkProgrammeApplication();
    scheduleWorkProgrammeApplication.setApplicationReference(appReference);

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

    return scheduleWorkProgrammeApplicationDetailRepository.countByVersionNumberAndStatusAndSubmittedDatetimeBetween(
        1,
        ScheduleWorkProgrammeApplicationStatus.SUBMITTED,
        startOfYear,
        endOfYear
    );
  }
}