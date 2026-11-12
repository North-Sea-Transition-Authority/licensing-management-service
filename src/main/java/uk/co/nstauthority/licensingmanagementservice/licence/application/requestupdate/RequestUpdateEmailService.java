package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactService;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryTeamService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@Service
public class RequestUpdateEmailService {

  static final String DOMAIN_REFERENCE_TYPE = "LicenceApplication";

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestUpdateEmailService.class);

  private final EmailService emailService;
  private final LicenceContactService licenceContactService;
  private final IndustryTeamService industryTeamService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public RequestUpdateEmailService(
      EmailService emailService,
      LicenceContactService licenceContactService,
      IndustryTeamService industryTeamService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.emailService = emailService;
    this.licenceContactService = licenceContactService;
    this.industryTeamService = industryTeamService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public void sendUpdateRequestedEmails(LicenceApplicationDetail updateDetail) {
    var licenceApplication = updateDetail.getLicenceApplication();

    var recipientEmails = getRecipientEmails(updateDetail);
    if (recipientEmails.isEmpty()) {
      LOGGER.warn("No recipients found to notify of update request for Application ID: {}", licenceApplication.getId());
      return;
    }

    var template = emailService.getTemplate(GovukNotifyTemplate.APPLICATION_UPDATE_REQUESTED_V1)
        .withMailMergeField("APPLICATION_TYPE", licenceApplication.getApplicationType().getDisplayName())
        .withMailMergeField("APPLICATION_REFERENCE", licenceApplication.getApplicationReference())
        .withMailMergeField("REQUEST_DEADLINE", DateFormatUtil.convertToDisplayText(updateDetail.getUpdateDeadlineDate()))
        .withMailMergeField("REQUEST_REASON", updateDetail.getUpdateReason());

    var mergedTemplate = template.merge();

    recipientEmails.forEach(email -> {
      try {
        emailService.sendEmail(
            mergedTemplate,
            EmailRecipient.directEmailAddress(email),
            DomainReference.from(licenceApplication.getId().toString(), DOMAIN_REFERENCE_TYPE)
        );
      } catch (Exception e) {
        LOGGER.error("Failed to send update requested email to {} for Application ID: {}",
            email, licenceApplication.getId(), e);
      }
    });
  }

  private Set<String> getRecipientEmails(LicenceApplicationDetail updateDetail) {
    var recipientEmails = new LinkedHashSet<String>();

    var organisationGroupId = organisationUnitQueryService
        .findOrganisationGroupIdByUnitId(updateDetail.getResponsibleOrganisationUnitId());
    if (organisationGroupId.isEmpty()) {
      LOGGER.warn("No organisation group found for organisation unit {}, submitters will not be notified of update request",
          updateDetail.getResponsibleOrganisationUnitId());
    } else {
      for (TeamMemberView submitter : industryTeamService.getSubmitterDetails(organisationGroupId.get())) {
        recipientEmails.add(submitter.email().toLowerCase());
      }
    }

    licenceContactService
        .getContactEmailForLicensee(updateDetail.getLicence(), updateDetail.getResponsibleOrganisationUnitId())
        .filter(StringUtils::isNotBlank)
        .ifPresent(contactEmail -> recipientEmails.add(contactEmail.toLowerCase()));

    return recipientEmails;
  }
}
