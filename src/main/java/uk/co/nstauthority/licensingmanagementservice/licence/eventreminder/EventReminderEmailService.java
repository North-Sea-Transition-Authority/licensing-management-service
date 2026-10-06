package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

@Service
public class EventReminderEmailService {

  static final String DOMAIN_REFERENCE_TYPE = "LICENCE_REMINDER";
  static final String DEADLINE_LINE = "* %s — %s";
  static final String TYPE_ONLY_DEADLINE_LINE = "* %s";

  private static final Comparator<EventReminderDeadline> DEADLINE_ORDER =
      Comparator.comparingInt((EventReminderDeadline deadline) -> deadline.reminderType().getDisplayOrder())
          .thenComparing(EventReminderDeadline::displayName);

  private final EmailService emailService;

  public EventReminderEmailService(EmailService emailService) {
    this.emailService = emailService;
  }

  public void queueReminder(
      EventReminderRecipient recipient,
      LocalDate deadlineDate,
      Collection<EventReminderDeadline> deadlines,
      UUID notificationBatchReference
  ) {
    if (deadlines.isEmpty()) {
      throw new IllegalArgumentException("Cannot queue a reminder with no deadlines");
    }

    var reminderTypes = deadlines.stream()
        .map(EventReminderDeadline::reminderType)
        .collect(Collectors.toSet());

    var mergedTemplate = emailService.getTemplate(getTemplate(reminderTypes))
        .withMailMergeField("LICENCE_REFERENCE", getLicenceReference(deadlines))
        .withMailMergeField("LICENSEE_NAME", recipient.licenseeName())
        .withMailMergeField("DEADLINE_DATE", DateFormatUtil.convertToDisplayText(deadlineDate))
        .withMailMergeField("DEADLINE_LIST", getDeadlineList(deadlines, reminderTypes))
        .merge();

    emailService.sendEmail(
        mergedTemplate,
        EmailRecipient.directEmailAddress(recipient.contactEmail()),
        DomainReference.from(notificationBatchReference.toString(), DOMAIN_REFERENCE_TYPE));
  }

  private GovukNotifyTemplate getTemplate(Set<ReminderType> reminderTypes) {
    if (reminderTypes.size() > 1) {
      return GovukNotifyTemplate.COMBINED_DEADLINE_REMINDER_V1;
    }

    return switch (reminderTypes.iterator().next()) {
      case TERM_OR_PHASE_END -> GovukNotifyTemplate.TERM_OR_PHASE_END_REMINDER_V1;
      case LICENCE_EXPIRY -> GovukNotifyTemplate.LICENCE_EXPIRY_REMINDER_V1;
      case WORK_PROGRAMME_ACTIVITY -> GovukNotifyTemplate.WORK_PROGRAMME_ACTIVITY_REMINDER_V1;
      case OTHER_SCHEDULE_EVENT -> GovukNotifyTemplate.OTHER_SCHEDULE_EVENT_REMINDER_V1;
    };
  }

  private String getLicenceReference(Collection<EventReminderDeadline> deadlines) {
    return deadlines.iterator().next().licence().getLicenceReference();
  }

  private String getDeadlineList(Collection<EventReminderDeadline> deadlines, Set<ReminderType> reminderTypes) {
    var isCombined = reminderTypes.size() > 1;

    return deadlines.stream()
        .sorted(DEADLINE_ORDER)
        .map(deadline -> isCombined ? getTypeNamedLine(deadline) : getLicenceNamedLine(deadline))
        .collect(Collectors.joining("\n"));
  }

  private String getLicenceNamedLine(EventReminderDeadline deadline) {
    return DEADLINE_LINE.formatted(deadline.licence().getLicenceReference(), deadline.displayName());
  }

  private String getTypeNamedLine(EventReminderDeadline deadline) {
    var typeName = deadline.reminderType().getDisplayName();

    return typeName.equals(deadline.displayName())
        ? TYPE_ONLY_DEADLINE_LINE.formatted(typeName)
        : DEADLINE_LINE.formatted(typeName, deadline.displayName());
  }
}
