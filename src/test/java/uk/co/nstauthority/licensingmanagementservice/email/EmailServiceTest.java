package uk.co.nstauthority.licensingmanagementservice.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MailMergeField;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.Template;
import uk.co.fivium.digitalnotificationlibrary.core.notification.TemplateType;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;

class EmailServiceTest {

  public static final GovukNotifyTemplate GOVUK_NOTIFY_TEMPLATE = GovukNotifyTemplate.SEND_CONTINUATION_ISSUED_DOCUMENT_V1;

  private static final Template TEMPLATE = new Template(
      GOVUK_NOTIFY_TEMPLATE.getTemplateId(),
      TemplateType.EMAIL,
      Set.of(),
      Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
  );

  private static final CustomerConfigurationProperties CUSTOMER_CONFIGURATION_PROPERTIES
      = new CustomerConfigurationProperties(
      "name", "mnemonic", "contactEmail", "approvals@nstauthority.co.uk"
  );

  private static NotificationLibraryClient notificationLibraryClient;
  private static EmailService emailService;

  @BeforeAll
  static void setup() {
    notificationLibraryClient = mock(NotificationLibraryClient.class);

    emailService = new EmailService(
        notificationLibraryClient,
        CUSTOMER_CONFIGURATION_PROPERTIES
    );
  }

  @Test
  void getTemplate_WhenInTestMode() {
    given(notificationLibraryClient.getTemplate(GOVUK_NOTIFY_TEMPLATE.getTemplateId()))
        .willReturn(TEMPLATE);
    given(notificationLibraryClient.isRunningTestMode())
        .willReturn(true);

    var resultingTemplate = emailService
        .getTemplate(GOVUK_NOTIFY_TEMPLATE)
        .merge();

    assertThat(resultingTemplate.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("SUBJECT_PREFIX", "***TEST***"),
            tuple("SALUTATION", "Dear"),
            tuple("VALEDICTION", "Kind regards"),
            tuple("REGULATOR_MNEMONIC", CUSTOMER_CONFIGURATION_PROPERTIES.mnemonic())
        );
  }

  @Test
  void getTemplate_WhenInProductionMode() {
    given(notificationLibraryClient.getTemplate(GOVUK_NOTIFY_TEMPLATE.getTemplateId()))
        .willReturn(TEMPLATE);
    given(notificationLibraryClient.isRunningProductionMode())
        .willReturn(true);

    var resultingTemplate = emailService
        .getTemplate(GOVUK_NOTIFY_TEMPLATE)
        .merge();

    assertThat(resultingTemplate.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("SUBJECT_PREFIX", ""),
            tuple("REGULATOR_MNEMONIC", CUSTOMER_CONFIGURATION_PROPERTIES.mnemonic()),
            tuple("SALUTATION", "Dear"),
            tuple("VALEDICTION", "Kind regards")
        );
  }

  @Test
  void sendEmail() {
    MergedTemplate mergedTemplate = MergedTemplate.builder(TEMPLATE).merge();
    CorrelationIdUtil.setCorrelationIdOnMdc("log-correlation-id");

    emailService.sendEmail(
        mergedTemplate,
        EmailRecipient.directEmailAddress("someone@example.com"),
        DomainReference.from("id", "type")
    );

    then(notificationLibraryClient)
        .should()
        .sendEmail(
            refEq(mergedTemplate),
            refEq(EmailRecipient.directEmailAddress("someone@example.com")),
            refEq(DomainReference.from("id", "type")),
            eq("log-correlation-id")
        );
  }
}