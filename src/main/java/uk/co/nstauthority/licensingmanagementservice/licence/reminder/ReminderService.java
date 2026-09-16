package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

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
public class ReminderService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReminderService.class);

  private final List<ReminderDeadlineSource> reminderDeadlineSources;
  private final ReminderSuppressionService reminderSuppressionService;
  private final ReminderRecipientService reminderRecipientService;
  private final ReminderBatchService reminderBatchService;
  private final LicenceReminderRepository licenceReminderRepository;

  public ReminderService(
      List<ReminderDeadlineSource> reminderDeadlineSources,
      ReminderSuppressionService reminderSuppressionService,
      ReminderRecipientService reminderRecipientService,
      ReminderBatchService reminderBatchService,
      LicenceReminderRepository licenceReminderRepository
  ) {
    this.reminderDeadlineSources = reminderDeadlineSources;
    this.reminderSuppressionService = reminderSuppressionService;
    this.reminderRecipientService = reminderRecipientService;
    this.reminderBatchService = reminderBatchService;
    this.licenceReminderRepository = licenceReminderRepository;
  }

  public ReminderRunSummary sendDueReminders() {
    var dueDeadlines = reminderDeadlineSources.stream()
        .flatMap(source -> source.getDeadlinesDueReminder().stream())
        .toList();

    if (dueDeadlines.isEmpty()) {
      LOGGER.info("No reminders due");
      return new ReminderRunSummary(0, 0, 0, 0);
    }

    var deadlines = getUnsuppressedDeadlines(dueDeadlines);

    var suppressedCount = dueDeadlines.size() - deadlines.size();

    if (deadlines.isEmpty()) {
      LOGGER.info("All {} candidate reminders were suppressed", dueDeadlines.size());
      return new ReminderRunSummary(dueDeadlines.size(), suppressedCount, 0, 0);
    }

    var recipientsByLicenceId = reminderRecipientService
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

    return new ReminderRunSummary(dueDeadlines.size(), suppressedCount, batches.size() - failed, failed);
  }

  private List<ReminderDeadline> getUnsuppressedDeadlines(List<ReminderDeadline> dueDeadlines) {
    var suppressedLicenceIds = reminderSuppressionService.getSuppressedLicenceIds(getDistinctLicences(dueDeadlines));

    return dueDeadlines.stream()
        .filter(deadline -> !suppressedLicenceIds.contains(deadline.licence().getId()))
        .toList();
  }

  private boolean queueBatch(BatchKey batchKey, List<ReminderDeadline> deadlines) {
    try {
      reminderBatchService.queueBatch(
          batchKey.recipient(), batchKey.deadlineDate(), deadlines, batchKey.reminderType());
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

  private Map<BatchKey, List<ReminderDeadline>> getBatches(
      List<ReminderDeadline> deadlines,
      Map<Integer, List<ReminderRecipient>> recipientsByLicenceId,
      Set<RemindedKey> alreadyReminded
  ) {
    var batches = new LinkedHashMap<BatchKey, List<ReminderDeadline>>();

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
            .computeIfAbsent(
                new BatchKey(recipient, deadline.deadlineDate(), deadline.reminderType()),
                key -> new ArrayList<>())
            .add(deadline);
      }
    }

    return batches;
  }

  private Set<RemindedKey> getAlreadyRemindedKeys(List<ReminderDeadline> deadlines) {
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

  private List<Licence> getDistinctLicences(List<ReminderDeadline> deadlines) {
    return deadlines.stream()
        .map(ReminderDeadline::licence)
        .distinct()
        .toList();
  }

  private record BatchKey(ReminderRecipient recipient, LocalDate deadlineDate, ReminderType reminderType) {
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
