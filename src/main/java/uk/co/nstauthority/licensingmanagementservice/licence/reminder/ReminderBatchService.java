package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderBatchService {

  private final Clock clock;
  private final LicenceReminderRepository licenceReminderRepository;
  private final ReminderEmailService reminderEmailService;

  public ReminderBatchService(
      Clock clock,
      LicenceReminderRepository licenceReminderRepository,
      ReminderEmailService reminderEmailService
  ) {
    this.clock = clock;
    this.licenceReminderRepository = licenceReminderRepository;
    this.reminderEmailService = reminderEmailService;
  }

  @Transactional
  public void queueBatch(
      ReminderRecipient recipient,
      LocalDate deadlineDate,
      Collection<ReminderDeadline> deadlines,
      NoticePeriod noticePeriod
  ) {
    var notificationBatchReference = UUID.randomUUID();
    var queuedAt = Instant.now(clock);

    var reminders = deadlines.stream()
        .map(deadline -> toReminder(deadline, recipient, noticePeriod, notificationBatchReference, queuedAt))
        .toList();

    licenceReminderRepository.saveAllAndFlush(reminders);

    reminderEmailService.queueReminder(recipient, deadlineDate, deadlines, notificationBatchReference);
  }

  private LicenceReminder toReminder(
      ReminderDeadline deadline,
      ReminderRecipient recipient,
      NoticePeriod noticePeriod,
      UUID notificationBatchReference,
      Instant queuedAt
  ) {
    var reminder = new LicenceReminder();
    reminder.setScheduleEvent(deadline.scheduleEvent());
    reminder.setOriginalEventId(deadline.originalEventId());
    reminder.setLicence(deadline.licence());
    reminder.setResponsibleOrganisationId(recipient.responsibleOrganisationId());
    reminder.setDeadlineDate(deadline.deadlineDate());
    reminder.setNoticePeriod(noticePeriod);
    reminder.setNotificationBatchReference(notificationBatchReference);
    reminder.setQueuedAt(queuedAt);
    return reminder;
  }
}
