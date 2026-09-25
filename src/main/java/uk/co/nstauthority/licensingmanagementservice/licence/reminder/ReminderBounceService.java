package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

@Service
public class ReminderBounceService {

  static final String DOMAIN_REFERENCE_TYPE = "LICENCE_REMINDER_BOUNCE";
  static final String BOUNCE_LINE = "* %s — %s — %s — %s due %s — %s";
  static final String UNKNOWN_LICENSEE = "Unknown licensee";

  private static final Logger LOGGER = LoggerFactory.getLogger(ReminderBounceService.class);

  private final Clock clock;
  private final LicenceReminderRepository licenceReminderRepository;
  private final LicenceReminderBounceRepository licenceReminderBounceRepository;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final CustomerConfigurationProperties customerConfigurationProperties;
  private final EmailService emailService;

  public ReminderBounceService(
      Clock clock,
      LicenceReminderRepository licenceReminderRepository,
      LicenceReminderBounceRepository licenceReminderBounceRepository,
      OrganisationUnitQueryService organisationUnitQueryService,
      CustomerConfigurationProperties customerConfigurationProperties,
      EmailService emailService
  ) {
    this.clock = clock;
    this.licenceReminderRepository = licenceReminderRepository;
    this.licenceReminderBounceRepository = licenceReminderBounceRepository;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.customerConfigurationProperties = customerConfigurationProperties;
    this.emailService = emailService;
  }

  @Transactional
  public int reportBounces() {
    var bouncedReminders = licenceReminderRepository.findAllBouncedAndUnreported();

    if (bouncedReminders.isEmpty()) {
      LOGGER.info("No bounced licence reminders to report");
      return 0;
    }

    var reportReference = UUID.randomUUID();
    var licenseeNamesByOrganisationId = getLicenseeNames(bouncedReminders);

    licenceReminderBounceRepository.saveAllAndFlush(toBounces(bouncedReminders));

    var mergedTemplate = emailService.getTemplate(GovukNotifyTemplate.REMINDER_BOUNCED_V1)
        .withMailMergeField("BOUNCE_COUNT", String.valueOf(bouncedReminders.size()))
        .withMailMergeField("BOUNCE_LIST", getBounceList(bouncedReminders, licenseeNamesByOrganisationId))
        .merge();

    emailService.sendEmail(
        mergedTemplate,
        EmailRecipient.directEmailAddress(customerConfigurationProperties.approvalsContactEmail()),
        DomainReference.from(reportReference.toString(), DOMAIN_REFERENCE_TYPE));

    LOGGER.info("Reported {} bounced licence reminders to {}",
        bouncedReminders.size(), customerConfigurationProperties.approvalsContactEmail());

    return bouncedReminders.size();
  }

  private Map<Integer, String> getLicenseeNames(List<BouncedReminder> bouncedReminders) {
    return organisationUnitQueryService.getOrganisationUnitNamesByIds(
        bouncedReminders.stream()
            .map(BouncedReminder::getResponsibleOrganisationId)
            .distinct()
            .toList());
  }

  private List<LicenceReminderBounce> toBounces(List<BouncedReminder> bouncedReminders) {
    var reportedAt = Instant.now(clock);

    return bouncedReminders.stream()
        .map(BouncedReminder::getNotificationBatchReference)
        .distinct()
        .map(notificationBatchReference -> toBounce(notificationBatchReference, reportedAt))
        .toList();
  }

  private LicenceReminderBounce toBounce(UUID notificationBatchReference, Instant reportedAt) {
    var bounce = new LicenceReminderBounce();
    bounce.setNotificationBatchReference(notificationBatchReference);
    bounce.setReportedAt(reportedAt);
    return bounce;
  }

  private String getBounceList(
      List<BouncedReminder> bouncedReminders,
      Map<Integer, String> licenseeNamesByOrganisationId
  ) {
    return bouncedReminders.stream()
        .sorted(Comparator.comparing(BouncedReminder::getLicenceReference)
            .thenComparing(BouncedReminder::getDeadlineDate))
        .map(bouncedReminder -> BOUNCE_LINE.formatted(
            bouncedReminder.getLicenceReference(),
            licenseeNamesByOrganisationId
                .getOrDefault(bouncedReminder.getResponsibleOrganisationId(), UNKNOWN_LICENSEE),
            bouncedReminder.getRecipient(),
            bouncedReminder.getReminderType().getDisplayName(),
            DateFormatUtil.convertToDisplayText(bouncedReminder.getDeadlineDate()),
            bouncedReminder.getFailureReason()))
        .collect(Collectors.joining("\n"));
  }
}
