package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.CaseManagerRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.LmsAbsoluteUrlUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class ScheduleWorkProgrammeApplicationSubmittedNotificationService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ScheduleWorkProgrammeApplicationSubmittedNotificationService.class);
  private static final String CASE_MANAGER_LOOKUP_PURPOSE = "Notify case managers of new schedule amendment application";

  private final EmailService emailService;
  private final TeamQueryService teamQueryService;
  private final EnergyPortalUserService energyPortalUserService;

  public ScheduleWorkProgrammeApplicationSubmittedNotificationService(
      EmailService emailService,
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.emailService = emailService;
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
  }

  public void sendNewApplicationSubmittedEmails(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var application = applicationDetail.getScheduleWorkProgrammeApplication();
    var licence = applicationDetail.getLicence();

    var requiredRole = CaseManagerRoles.getRequiredRoleForLicenceType(licence.getType());
    if (requiredRole.isEmpty()) {
      return;
    }

    var wuaIds = teamQueryService.getAllTeamRolesWithRoles(Set.of(requiredRole.get())).stream()
        .map(teamRole -> WebUserAccountId.from(teamRole.getWuaId()))
        .distinct()
        .toList();

    var caseManagers = energyPortalUserService.findByWuaIds(wuaIds, CASE_MANAGER_LOOKUP_PURPOSE);
    if (caseManagers.isEmpty()) {
      LOGGER.warn("No {} users found to notify of new application submission for Application ID: {}",
          requiredRole.get().getName(), application.getId());
      return;
    }

    var overviewController = on(ScheduleWorkProgrammeApplicationOverviewController.class);
    var overviewUrl = ReverseRouter.route(overviewController.renderOverview(applicationDetail.getId(), null, null));
    var applicationUrl = LmsAbsoluteUrlUtil.getAbsoluteUrl(overviewUrl);

    var template = emailService.getTemplate(GovukNotifyTemplate.NEW_SCHEDULE_AMENDMENT_APPLICATION_SUBMITTED_V1)
        .withMailMergeField("APPLICATION_TYPE", ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName())
        .withMailMergeField("APPLICATION_REFERENCE", application.getApplicationReference())
        .withMailMergeField("LICENCE_REFERENCE", licence.getLicenceReference())
        .withMailMergeField("APPLICATION_LINK", applicationUrl);

    for (var caseManager : caseManagers) {
      var mergedTemplate = template.withMailMergeField("USER_NAME", caseManager.displayName()).merge();

      try {
        emailService.sendEmail(
            mergedTemplate,
            EmailRecipient.directEmailAddress(caseManager.emailAddress()),
            DomainReference.from(application.getId().toString(), WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION.name())
        );
      } catch (Exception e) {
        LOGGER.error("Failed to send new application submitted email to {} for Application ID: {}",
            caseManager.emailAddress(), application.getId(), e);
      }
    }
  }
}
