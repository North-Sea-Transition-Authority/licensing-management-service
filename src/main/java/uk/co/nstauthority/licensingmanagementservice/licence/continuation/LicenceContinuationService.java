package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentItemType;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw.ApplicationWithdrawService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryTeamService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.ClearDownWorkAreaLogService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class LicenceContinuationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenceContinuationService.class);

  private static final String APPLICATION_REFERENCE_FORMAT = "LMS/CA/%d/%d";

  private final ApplicationAccessService applicationAccessService;
  public static final String LICENCE_CONTINUATION_APPLICATION_DETAIL = "licence continuation application detail";
  public final LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;
  private final LicenceContinuationApplicationRepository licenceContinuationApplicationRepository;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final Clock clock;
  private final ApplicationLetterService applicationLetterService;
  private final ApplicationWithdrawService applicationWithdrawService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final EmailService emailService;
  private final IndustryTeamService industryTeamService;
  private final ClearDownWorkAreaLogService clearDownWorkAreaLogService;

  public LicenceContinuationService(
      LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository,
      LicenceContinuationApplicationRepository licenceContinuationApplicationRepository,
      LicenceScheduleDetailService licenceScheduleDetailService,
      Clock clock,
      ApplicationAccessService applicationAccessService,
      ApplicationLetterService applicationLetterService,
      ApplicationWithdrawService applicationWithdrawService,
      OrganisationUnitQueryService organisationUnitQueryService,
      EmailService emailService,
      IndustryTeamService industryTeamService,
      ClearDownWorkAreaLogService clearDownWorkAreaLogService
  ) {
    this.licenceContinuationApplicationDetailRepository = licenceContinuationApplicationDetailRepository;
    this.licenceContinuationApplicationRepository = licenceContinuationApplicationRepository;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.clock = clock;
    this.applicationAccessService = applicationAccessService;
    this.applicationLetterService = applicationLetterService;
    this.applicationWithdrawService = applicationWithdrawService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.emailService = emailService;
    this.industryTeamService = industryTeamService;
    this.clearDownWorkAreaLogService = clearDownWorkAreaLogService;
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

    return licenceContinuationApplicationDetailRepository.countByVersionNumberAndSubmittedDatetimeBetween(
        1,
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

  public void sendContinuationIssuanceEmails(
      LicenceApplication application,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var orgGroupId = getOrgGroupIdForContinuationApplication(licenceContinuationApplicationDetail);

    var submitters = industryTeamService.getSubmitterDetails(orgGroupId);

    if (submitters == null || submitters.isEmpty()) {
      return;
    }

    var applicationId = application.getId();
    MergedTemplate.MergedTemplateBuilder template = emailService.getTemplate(
        GovukNotifyTemplate.SEND_CONTINUATION_ISSUED_DOCUMENT_V1
        )
        .withMailMergeField("APPLICATION_REFERENCE", application.getId().toString())
        .withMailMergeField(
            "DOCUMENT_OVERVIEW_LINK",
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(
                applicationId,
                licenceContinuationApplicationDetail,
                null,
                OverviewTab.LETTER
            ))
        );

    for (TeamMemberView submitter : submitters) {
      var mergedTemplate =  template.withMailMergeField("USER_NAME", submitter.getDisplayName()).merge();

      try {
        emailService.sendEmail(
            mergedTemplate,
            EmailRecipient.directEmailAddress(submitter.email()),
            DomainReference.from(application.getId().toString(), DocumentItemType.CONTINUATION_LETTER.name())
        );
      } catch (Exception e) {
        LOGGER.error("Failed to send Continuation issuance email to {} for Application ID: {}",
                     submitter.email(), application.getId(), e);
      }
    }
  }

  @Transactional
  public void withdrawContinuationChangeStatus(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      String withdrawalReason
  ) {
    licenceContinuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.WITHDRAWN);
    licenceContinuationApplicationDetailRepository.save(licenceContinuationApplicationDetail);

    var application = licenceContinuationApplicationDetail.getLicenceContinuationApplication();
    application.setWithdrawalReason(withdrawalReason);
    licenceContinuationApplicationRepository.save(application);

    var orgGroupId = getOrgGroupIdForContinuationApplication(licenceContinuationApplicationDetail);

    var submitters = industryTeamService.getSubmitterDetails(orgGroupId);

    applicationWithdrawService.sendApplicationWithdrawnEmails(
        application.getWithdrawalReason(),
        submitters,
        "CONTINUATION_WITHDRAWAL",
        application
    );

  }

  @Transactional
  public void issueContinuationLetter(LicenceApplication application) {
    var continuationApplicationDetail = getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(application.getId());

    continuationApplicationDetail.setStatus(LicenceContinuationApplicationStatus.COMPLETE);
    licenceContinuationApplicationDetailRepository.save(continuationApplicationDetail);

    clearDownWorkAreaLogService.clearDownAllViewsFor(
        continuationApplicationDetail.getId(),
        WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION
    );

    sendContinuationIssuanceEmails(application, continuationApplicationDetail);
  }

  private Integer getOrgGroupIdForContinuationApplication(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    var orgGroupId = organisationUnitQueryService.findOrganisationGroupIdByUnitId(
        licenceContinuationApplicationDetail.getResponsibleOrganisationUnitId()
    );

    if (orgGroupId.isPresent()) {
      return orgGroupId.get();
    } else {
      throw new LmsEntityNotFoundException(
          "organisation group",
          licenceContinuationApplicationDetail.getResponsibleOrganisationUnitId()
      );
    }
  }
}