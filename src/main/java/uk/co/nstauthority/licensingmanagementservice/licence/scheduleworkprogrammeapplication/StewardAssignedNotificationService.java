package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.LmsAbsoluteUrlUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class StewardAssignedNotificationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(StewardAssignedNotificationService.class);
  private static final String STEWARD_LOOKUP_PURPOSE = "Notify steward of case assignment";

  private final EmailService emailService;
  private final EnergyPortalUserService energyPortalUserService;

  public StewardAssignedNotificationService(
      EmailService emailService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.emailService = emailService;
    this.energyPortalUserService = energyPortalUserService;
  }

  public void sendCaseAssignedEmail(ScheduleWorkProgrammeApplicationDetail applicationDetail, Long stewardWuaId) {
    var application = applicationDetail.getScheduleWorkProgrammeApplication();
    var licence = applicationDetail.getLicence();

    var steward = energyPortalUserService
        .findByWuaIds(List.of(WebUserAccountId.from(stewardWuaId)), STEWARD_LOOKUP_PURPOSE)
        .stream()
        .findFirst();

    if (steward.isEmpty()) {
      LOGGER.warn("No user found for wuaId {} to notify of case assignment for Application ID: {}",
          stewardWuaId, application.getId());
      return;
    }

    var overviewController = on(ScheduleWorkProgrammeApplicationOverviewController.class);
    var overviewUrl = ReverseRouter.route(overviewController.renderOverview(applicationDetail.getId(), null, null));
    var applicationUrl = LmsAbsoluteUrlUtil.getAbsoluteUrl(overviewUrl);

    var mergedTemplate = emailService.getTemplate(GovukNotifyTemplate.STEWARD_ASSIGNED_TO_APPLICATION_V1)
        .withMailMergeField("APPLICATION_TYPE", ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName())
        .withMailMergeField("APPLICATION_REFERENCE", application.getApplicationReference())
        .withMailMergeField("LICENCE_REFERENCE", licence.getLicenceReference())
        .withMailMergeField("APPLICATION_LINK", applicationUrl)
        .withMailMergeField("USER_NAME", steward.get().displayName())
        .merge();

    try {
      emailService.sendEmail(
          mergedTemplate,
          EmailRecipient.directEmailAddress(steward.get().emailAddress()),
          DomainReference.from(application.getId().toString(), WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION.name())
      );
    } catch (Exception e) {
      LOGGER.error("Failed to send case assignment email to {} for Application ID: {}",
          steward.get().emailAddress(), application.getId(), e);
    }
  }
}
