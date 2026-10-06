package uk.co.nstauthority.licensingmanagementservice.licence.eventreminder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class EventReminderService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventReminderService.class);

  private final List<EventReminderDeadlineSource> eventReminderDeadlineSources;
  private final EventReminderSuppressionService eventReminderSuppressionService;
  private final EventReminderRecipientService eventReminderRecipientService;
  private final EventReminderBatchService eventReminderBatchService;
  private final LicenceReminderRepository licenceReminderRepository;
  private final EventReminderMissingContactService eventReminderMissingContactService;

  public EventReminderService(
      List<EventReminderDeadlineSource> eventReminderDeadlineSources,
      EventReminderSuppressionService eventReminderSuppressionService,
      EventReminderRecipientService eventReminderRecipientService,
      EventReminderBatchService eventReminderBatchService,
      LicenceReminderRepository licenceReminderRepository,
      EventReminderMissingContactService eventReminderMissingContactService
  ) {
    this.eventReminderDeadlineSources = eventReminderDeadlineSources;
    this.eventReminderSuppressionService = eventReminderSuppressionService;
    this.eventReminderRecipientService = eventReminderRecipientService;
    this.eventReminderBatchService = eventReminderBatchService;
    this.licenceReminderRepository = licenceReminderRepository;
    this.eventReminderMissingContactService = eventReminderMissingContactService;
  }

  public EventReminderRunSummary sendDueReminders() {
    var dueDeadlines = eventReminderDeadlineSources.stream()
        .flatMap(source -> source.getDeadlinesDueReminder().stream())
        .toList();

    if (dueDeadlines.isEmpty()) {
      LOGGER.info("No reminders due");
      return new EventReminderRunSummary(0, 0, 0, 0);
    }

    var deadlines = getUnsuppressedDeadlines(dueDeadlines);

    var suppressedCount = dueDeadlines.size() - deadlines.size();

    if (deadlines.isEmpty()) {
      LOGGER.info("All {} candidate reminders were suppressed", dueDeadlines.size());
      return new EventReminderRunSummary(dueDeadlines.size(), suppressedCount, 0, 0);
    }

    var recipientsByLicenceId = eventReminderRecipientService
        .getRecipientsByLicenceId(getDistinctLicences(deadlines));

    var alreadyReminded = getAlreadyRemindedKeys(deadlines);
    var batches = getBatches(deadlines, recipientsByLicenceId, alreadyReminded);

    LOGGER.info("Queueing {} reminder batches", batches.size());

    var failed = 0;

    for (var batch : batches.entrySet()) {
      if (!queueBatch(batch.getKey(), batch.getValue())) {
        failed++;
      }
    }

    LOGGER.info("Queued {} of {} reminder batches", batches.size() - failed, batches.size());

    reportMissingContacts(deadlines);

    return new EventReminderRunSummary(dueDeadlines.size(), suppressedCount, batches.size() - failed, failed);
  }

  private void reportMissingContacts(List<EventReminderDeadline> deadlines) {
    try {
      eventReminderMissingContactService.reportMissingContacts(deadlines);
    } catch (Exception e) {
      LOGGER.error("Failed to report licensees with no contact, reminders were not affected", e);
    }
  }

  private List<EventReminderDeadline> getUnsuppressedDeadlines(List<EventReminderDeadline> dueDeadlines) {
    var suppressedLicenceIds = eventReminderSuppressionService.getSuppressedLicenceIds(getDistinctLicences(dueDeadlines));

    return dueDeadlines.stream()
        .filter(deadline -> !suppressedLicenceIds.contains(deadline.licence().getId()))
        .toList();
  }

  private boolean queueBatch(BatchKey batchKey, List<EventReminderDeadline> deadlines) {
    try {
      eventReminderBatchService.queueBatch(batchKey.recipient(), batchKey.deadlineDate(), deadlines);
      return true;
    } catch (Exception e) {
      LOGGER.error(
          "Failed to queue reminder for organisation {} on licence {} for {}",
          batchKey.recipient().responsibleOrganisationId(),
          batchKey.recipient().licenceId(),
          batchKey.deadlineDate(),
          e);
      return false;
    }
  }

  private Map<BatchKey, List<EventReminderDeadline>> getBatches(
      List<EventReminderDeadline> deadlines,
      Map<Integer, List<EventReminderRecipient>> recipientsByLicenceId,
      Set<RemindedKey> alreadyReminded
  ) {
    var batches = new LinkedHashMap<BatchKey, List<EventReminderDeadline>>();

    for (var deadline : deadlines) {
      for (var recipient : recipientsByLicenceId.getOrDefault(deadline.licence().getId(), List.of())) {
        var remindedKey = new RemindedKey(
            deadline.originalEventId(),
            deadline.licence().getId(),
            deadline.reminderType(),
            recipient.responsibleOrganisationId(),
            deadline.deadlineDate());

        if (alreadyReminded.contains(remindedKey)) {
          continue;
        }

        batches
            .computeIfAbsent(new BatchKey(recipient, deadline.deadlineDate()), key -> new ArrayList<>())
            .add(deadline);
      }
    }

    return batches;
  }

  private Set<RemindedKey> getAlreadyRemindedKeys(List<EventReminderDeadline> deadlines) {
    return licenceReminderRepository
        .findAllByLicenceIn(getDistinctLicences(deadlines))
        .stream()
        .map(reminder -> new RemindedKey(
            reminder.getOriginalEventId(),
            reminder.getLicence().getId(),
            reminder.getReminderType(),
            reminder.getResponsibleOrganisationId(),
            reminder.getDeadlineDate()))
        .collect(Collectors.toSet());
  }

  private List<Licence> getDistinctLicences(List<EventReminderDeadline> deadlines) {
    return deadlines.stream()
        .map(EventReminderDeadline::licence)
        .distinct()
        .toList();
  }

  private record BatchKey(EventReminderRecipient recipient, LocalDate deadlineDate) {
  }

  private record RemindedKey(
      UUID originalEventId,
      Integer licenceId,
      ReminderType reminderType,
      Integer responsibleOrganisationId,
      LocalDate deadlineDate
  ) {
  }
}
