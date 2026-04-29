package uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@Service
public class ApplicationWithdrawService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationWithdrawService.class);

  private final EmailService emailService;

  public ApplicationWithdrawService(EmailService emailService) {
    this.emailService = emailService;
  }

  public void sendApplicationWithdrawnEmails(
      String withdrawalReason,
      List<TeamMemberView> submitters,
      String domainReferenceType,
      LicenceApplication licenceApplication
  ) {
    if (submitters == null || submitters.isEmpty()) {
      return;
    }

    MergedTemplate.MergedTemplateBuilder template = emailService.getTemplate(
            GovukNotifyTemplate.APPLICATION_WITHDRAWAL_V1
        )
        .withMailMergeField("APPLICATION_TYPE", licenceApplication.getApplicationType().getDisplayName())
        .withMailMergeField("APPLICATION_REFERENCE", licenceApplication.getApplicationReference())
        .withMailMergeField("WITHDRAWAL_REASON", withdrawalReason);

    for (TeamMemberView submitter : submitters) {
      var mergedTemplate = template.withMailMergeField("USER_NAME", submitter.getDisplayName()).merge();

      try {
        emailService.sendEmail(
            mergedTemplate,
            EmailRecipient.directEmailAddress(submitter.email()),
            DomainReference.from(licenceApplication.getId().toString(), domainReferenceType)
        );
      } catch (Exception e) {
        LOGGER.error("Failed to send {} withdrawn email to {} for Application ID: {}",
                     licenceApplication.getApplicationType().getDisplayName(), submitter.email(), licenceApplication.getId(), e);
      }
    }
  }

}
