package uk.co.nstauthority.licensingmanagementservice.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailNotification;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;

@Service
public class EmailService {

  private final NotificationLibraryClient notificationLibraryClient;
  private final CustomerConfigurationProperties customerConfigurationProperties;

  @Autowired
  public EmailService(NotificationLibraryClient notificationLibraryClient,
                      CustomerConfigurationProperties customerConfigurationProperties) {
    this.notificationLibraryClient = notificationLibraryClient;
    this.customerConfigurationProperties = customerConfigurationProperties;
  }

  public MergedTemplate.MergedTemplateBuilder getTemplate(GovukNotifyTemplate notifyTemplate) {

    var subjectPrefix = notificationLibraryClient.isRunningTestMode() ? "***TEST***" : "";

    return notificationLibraryClient.getTemplate(notifyTemplate.getTemplateId())
        .withMailMergeField("SUBJECT_PREFIX", subjectPrefix)
        .withMailMergeField("SALUTATION", "Dear")
        .withMailMergeField("VALEDICTION", "Kind regards")
        .withMailMergeField("REGULATOR_MNEMONIC", customerConfigurationProperties.mnemonic());
  }

  public EmailNotification sendEmail(MergedTemplate mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference) {
    return notificationLibraryClient.sendEmail(
        mergedTemplate,
        recipient,
        domainReference,
        CorrelationIdUtil.getLogCorrelationId().id()
    );
  }
}
