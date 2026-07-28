package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;

@Service
public class LicenceContactEmailService {

  static final String DOMAIN_REFERENCE_TYPE = "LICENCE_CONTACT";

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenceContactEmailService.class);

  private final EmailService emailService;

  public LicenceContactEmailService(EmailService emailService) {
    this.emailService = emailService;
  }

  public void sendContactUpdatedEmail(
      String contactEmail,
      String licenseeName,
      List<String> licenceReferences,
      Integer organisationId
  ) {
    var mergedTemplate = emailService.getTemplate(GovukNotifyTemplate.LICENCE_CONTACT_UPDATED_V1)
        .withMailMergeField("LICENCE_REFERENCES", String.join(", ", licenceReferences))
        .withMailMergeField("LICENSEE_NAME", licenseeName)
        .merge();

    try {
      emailService.sendEmail(
          mergedTemplate,
          EmailRecipient.directEmailAddress(contactEmail),
          DomainReference.from(organisationId.toString(), DOMAIN_REFERENCE_TYPE)
      );
    } catch (Exception e) {
      LOGGER.error("Failed to send licence contact updated email to {} for organisation ID: {}",
                   contactEmail, organisationId, e);
    }
  }
}
