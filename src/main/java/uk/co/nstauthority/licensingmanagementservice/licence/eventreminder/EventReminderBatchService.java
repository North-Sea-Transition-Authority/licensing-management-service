package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventReminderBatchService {

  private final Clock clock;
  private final LicenceReminderRepository licenceReminderRepository;
  private final EventReminderEmailService eventReminderEmailService;

  public EventReminderBatchService(
      Clock clock,
      LicenceReminderRepository licenceReminderRepository,
      EventReminderEmailService eventReminderEmailService
  ) {
    this.clock = clock;
    this.licenceReminderRepository = licenceReminderRepository;
    this.eventReminderEmailService = eventReminderEmailService;
  }

  @Transactional
  public void queueBatch(
      EventReminderRecipient recipient,
      LocalDate deadlineDate,
      Collection<EventReminderDeadline> deadlines
  ) {
    var notificationBatchReference = UUID.randomUUID();
    var queuedAt = Instant.now(clock);

    var reminders = deadlines.stream()
        .map(deadline -> toReminder(deadline, recipient, notificationBatchReference, queuedAt))
        .toList();

    licenceReminderRepository.saveAllAndFlush(reminders);

    eventReminderEmailService.queueReminder(recipient, deadlineDate, deadlines, notificationBatchReference);
  }

  private LicenceReminder toReminder(
      EventReminderDeadline deadline,
      EventReminderRecipient recipient,
      UUID notificationBatchReference,
      Instant queuedAt
  ) {
    var reminder = new LicenceReminder();
    reminder.setScheduleEvent(deadline.scheduleEvent());
    reminder.setOriginalEventId(deadline.originalEventId());
    reminder.setLicence(deadline.licence());
    reminder.setResponsibleOrganisationId(recipient.responsibleOrganisationId());
    reminder.setDeadlineDate(deadline.deadlineDate());
    reminder.setNoticePeriod(deadline.reminderType().getNoticePeriod());
    reminder.setReminderType(deadline.reminderType());
    reminder.setNotificationBatchReference(notificationBatchReference);
    reminder.setQueuedAt(queuedAt);
    return reminder;
  }
}
