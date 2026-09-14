package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.energyportalmessagequeue.message.EpmqMessageTypeMapping.getTypeToClassMapByTopic;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalmessagequeue.message.EpmqMessage;
import uk.co.fivium.energyportalmessagequeue.message.EpmqTopics;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContacts;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsLicenceContactsEpmqMessage;
import uk.co.fivium.energyportalmessagequeue.message.pears.PearsTransactionAppliedEpmqMessage;
import uk.co.fivium.energyportalmessagequeue.sns.SnsService;
import uk.co.fivium.energyportalmessagequeue.sns.SnsTopicArn;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsQueueUrl;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsService;

@ExtendWith(MockitoExtension.class)
class PearsLicenceContactsSubscriptionServiceTest {

  private static final SqsQueueUrl QUEUE_URL = new SqsQueueUrl("queue-url");
  private static final SnsTopicArn TOPIC_ARN = new SnsTopicArn("topic-arn");
  private static final Map<String, Class<? extends EpmqMessage>> PEARS_LICENCES_MESSAGE_TYPES =
      getTypeToClassMapByTopic(EpmqTopics.PEARS_LICENCES);

  @Mock
  private SqsService sqsService;

  @Mock
  private SnsService snsService;

  @Mock
  private PearsLicenceContactsUpdateService pearsLicenceContactsUpdateService;

  @Captor
  private ArgumentCaptor<Consumer<EpmqMessage>> onMessageCaptor;

  private PearsLicenceContactsSubscriptionService pearsLicenceContactsSubscriptionService;

  @BeforeEach
  void setUp() {
    when(sqsService.getOrCreateQueue(PearsLicenceContactsSubscriptionService.QUEUE_NAME)).thenReturn(QUEUE_URL);
    when(snsService.getOrCreateTopic(EpmqTopics.PEARS_LICENCES.getName())).thenReturn(TOPIC_ARN);

    pearsLicenceContactsSubscriptionService =
        new PearsLicenceContactsSubscriptionService(sqsService, snsService, pearsLicenceContactsUpdateService);
  }

  @Test
  void constructor_subscribesQueueToPearsLicencesTopic() {
    verify(snsService).subscribeTopicToSqsQueue(TOPIC_ARN, QUEUE_URL);
  }

  @Test
  void receiveQueueMessages_whenLicenceContactsMessage_thenContactsUpdated() {
    var message = new PearsLicenceContactsEpmqMessage(
        "1", new PearsLicenceContacts(List.of()), "correlation-id", Instant.now());

    receiveMessage(message);

    verify(pearsLicenceContactsUpdateService).updateContacts(message);
  }

  @Test
  void receiveQueueMessages_whenOtherPearsLicencesMessage_thenIgnored() {
    receiveMessage(new PearsTransactionAppliedEpmqMessage());

    verifyNoInteractions(pearsLicenceContactsUpdateService);
  }

  private void receiveMessage(EpmqMessage message) {
    pearsLicenceContactsSubscriptionService.receiveQueueMessages();

    verify(sqsService)
        .receiveQueueMessages(eq(QUEUE_URL), eq(PEARS_LICENCES_MESSAGE_TYPES), onMessageCaptor.capture());

    onMessageCaptor.getValue().accept(message);
  }
}
