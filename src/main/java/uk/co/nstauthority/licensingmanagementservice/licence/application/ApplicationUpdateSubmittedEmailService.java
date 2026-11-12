package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.LmsAbsoluteUrlUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class ApplicationUpdateSubmittedEmailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationUpdateSubmittedEmailService.class);
  private static final String CASE_MANAGER_LOOKUP_PURPOSE = "Notify case managers of a submitted application update";

  private final EmailService emailService;
  private final TeamQueryService teamQueryService;
  private final EnergyPortalUserService energyPortalUserService;

  public ApplicationUpdateSubmittedEmailService(
      EmailService emailService,
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.emailService = emailService;
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
  }

  public void sendUpdateSubmittedEmails(LicenceApplicationDetail applicationDetail) {
    var application = applicationDetail.getLicenceApplication();
    var licence = applicationDetail.getLicence();

    var requiredRole = CaseManagerRoles.getRequiredRoleForLicenceType(licence.getType());
    if (requiredRole.isEmpty()) {
      return;
    }

    var wuaIds = Stream.concat(
            teamQueryService.getAllTeamRolesWithRoles(Set.of(requiredRole.get())).stream()
                .map(TeamRole::getWuaId),
            getAssignedStewardWuaId(application).stream()
        )
        .map(WebUserAccountId::from)
        .distinct()
        .toList();

    var caseManagers = energyPortalUserService.findByWuaIds(wuaIds, CASE_MANAGER_LOOKUP_PURPOSE);
    if (caseManagers.isEmpty()) {
      LOGGER.warn("No {} users found to notify of application update submission for Application ID: {}",
          requiredRole.get().getName(), application.getId());
      return;
    }

    var template = emailService.getTemplate(GovukNotifyTemplate.APPLICATION_UPDATE_SUBMITTED_V1)
        .withMailMergeField("APPLICATION_TYPE", application.getApplicationType().getDisplayName().toLowerCase())
        .withMailMergeField("APPLICATION_REFERENCE", application.getApplicationReference())
        .withMailMergeField("LICENCE_REFERENCE", licence.getLicenceReference())
        .withMailMergeField("APPLICATION_LINK", getApplicationUrl(applicationDetail));

    var domainType = getDomainType(application.getApplicationType());

    for (var caseManager : caseManagers) {
      var mergedTemplate = template.withMailMergeField("USER_NAME", caseManager.displayName()).merge();

      try {
        emailService.sendEmail(
            mergedTemplate,
            EmailRecipient.directEmailAddress(caseManager.emailAddress()),
            DomainReference.from(application.getId().toString(), domainType.name())
        );
      } catch (Exception e) {
        LOGGER.error("Failed to send application update submitted email to {} for Application ID: {}",
            caseManager.emailAddress(), application.getId(), e);
      }
    }
  }

  private Optional<Long> getAssignedStewardWuaId(LicenceApplication application) {
    if (application instanceof ScheduleWorkProgrammeApplication scheduleWorkProgrammeApplication) {
      return Optional.ofNullable(scheduleWorkProgrammeApplication.getStewardWuaId());
    }
    return Optional.empty();
  }

  private String getApplicationUrl(LicenceApplicationDetail applicationDetail) {
    var id = applicationDetail.getId();
    var overviewUrl = switch (applicationDetail.getLicenceApplication().getApplicationType()) {
      case SCHEDULE_AMENDMENT_APPLICATION -> ReverseRouter.route(
          on(ScheduleWorkProgrammeApplicationOverviewController.class).renderOverview(id, null, null, null));
      case CONTINUATION_APPLICATION -> ReverseRouter.route(
          on(LicenceContinuationApplicationOverviewController.class).renderOverview(id, null, null, null));
      case LICENCE_ASSIGNMENT_APPLICATION ->
          throw new IllegalStateException("Licence assignment applications cannot be updated yet");
    };
    return LmsAbsoluteUrlUtil.getAbsoluteUrl(overviewUrl);
  }

  private WorkAreaDataItemType getDomainType(ApplicationType applicationType) {
    return switch (applicationType) {
      case SCHEDULE_AMENDMENT_APPLICATION -> WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION;
      case CONTINUATION_APPLICATION -> WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION;
      case LICENCE_ASSIGNMENT_APPLICATION ->
          throw new IllegalStateException("Licence assignment applications are not in the work area yet");
    };
  }
}
