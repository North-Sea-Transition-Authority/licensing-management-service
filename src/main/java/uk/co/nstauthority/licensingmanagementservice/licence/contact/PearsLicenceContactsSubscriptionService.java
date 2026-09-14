package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportalmessagequeue.message.EpmqMessage;
import uk.co.fivium.energyportalmessagequeue.message.EpmqMessageTypeMapping;
import uk.co.fivium.energyportalmessagequeue.message.EpmqTopics;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContactsEpmqMessage;
import uk.co.fivium.energyportalmessagequeue.sns.SnsService;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsQueueUrl;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsService;

/**
 * Subscribes LMS to the PEARS licences EPMQ topic and polls its queue for the licence contacts PEARS publishes.
 *
 * <p>The topic carries every PEARS licence message, not just licence contacts, so all of the topic's message types
 * are mapped in order that the ones LMS does not act on still deserialize and get acknowledged rather than being
 * redelivered.</p>
 */
@Service
public class PearsLicenceContactsSubscriptionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsLicenceContactsSubscriptionService.class);

  static final String QUEUE_NAME = "pears-licences-lms";

  private final SqsService sqsService;
  private final SqsQueueUrl queueUrl;
  private final PearsLicenceContactsUpdateService pearsLicenceContactsUpdateService;

  PearsLicenceContactsSubscriptionService(
      SqsService sqsService,
      SnsService snsService,
      PearsLicenceContactsUpdateService pearsLicenceContactsUpdateService
  ) {
    this.sqsService = sqsService;
    this.pearsLicenceContactsUpdateService = pearsLicenceContactsUpdateService;

    queueUrl = sqsService.getOrCreateQueue(QUEUE_NAME);
    snsService.subscribeTopicToSqsQueue(snsService.getOrCreateTopic(EpmqTopics.PEARS_LICENCES.getName()), queueUrl);
  }

  /**
   * Polls the queue for new messages. Each poll long polls for up to 20 seconds, which is why
   * {@code spring.task.scheduling.pool.size} is raised above the Spring default of one thread.
   */
  @Scheduled(fixedDelayString = "PT5S")
  void receiveQueueMessages() {
    sqsService.receiveQueueMessages(
        queueUrl,
        EpmqMessageTypeMapping.getTypeToClassMapByTopic(EpmqTopics.PEARS_LICENCES),
        (EpmqMessage message) -> {
          if (message instanceof PearsLicenceContactsEpmqMessage licenceContactsMessage) {
            pearsLicenceContactsUpdateService.updateContacts(licenceContactsMessage);
          } else {
            LOGGER.debug("Ignoring {} from the {} topic", message.getType(), EpmqTopics.PEARS_LICENCES.getName());
          }
        }
    );
  }
}
