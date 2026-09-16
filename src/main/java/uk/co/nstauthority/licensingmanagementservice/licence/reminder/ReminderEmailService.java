package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

@Service
public class ReminderEmailService {

  static final String DOMAIN_REFERENCE_TYPE = "LICENCE_REMINDER";
  static final String DEADLINE_LINE = "* %s — %s";

  private final EmailService emailService;

  public ReminderEmailService(EmailService emailService) {
    this.emailService = emailService;
  }

  public void queueReminder(
      ReminderRecipient recipient,
      LocalDate deadlineDate,
      Collection<ReminderDeadline> deadlines,
      UUID notificationBatchReference
  ) {
    if (deadlines.isEmpty()) {
      throw new IllegalArgumentException("Cannot queue a reminder with no deadlines");
    }

    var mergedTemplate = emailService.getTemplate(GovukNotifyTemplate.TERM_OR_PHASE_END_REMINDER_V1)
        .withMailMergeField("LICENCE_REFERENCE", getLicenceReference(deadlines))
        .withMailMergeField("LICENSEE_NAME", recipient.licenseeName())
        .withMailMergeField("DEADLINE_DATE", DateFormatUtil.convertToDisplayText(deadlineDate))
        .withMailMergeField("DEADLINE_LIST", getDeadlineList(deadlines))
        .merge();

    emailService.sendEmail(
        mergedTemplate,
        EmailRecipient.directEmailAddress(recipient.contactEmail()),
        DomainReference.from(notificationBatchReference.toString(), DOMAIN_REFERENCE_TYPE));
  }

  private String getLicenceReference(Collection<ReminderDeadline> deadlines) {
    return deadlines.iterator().next().licence().getLicenceReference();
  }

  private String getDeadlineList(Collection<ReminderDeadline> deadlines) {
    return deadlines.stream()
        .sorted(Comparator.comparing(ReminderDeadline::displayName))
        .map(deadline -> DEADLINE_LINE.formatted(
            deadline.licence().getLicenceReference(),
            deadline.displayName()))
        .collect(Collectors.joining("\n"));
  }
}
