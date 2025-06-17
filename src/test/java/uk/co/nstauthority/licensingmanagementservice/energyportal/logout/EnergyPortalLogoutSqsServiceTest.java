package uk.co.nstauthority.licensingmanagementservice.energyportal.logout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportal.accounts.epmq.EnergyPortalAccountsTopic;
import uk.co.fivium.energyportal.accounts.epmq.messages.UserSignedOutEpasMessage;
import uk.co.fivium.energyportalmessagequeue.sns.SnsService;
import uk.co.fivium.energyportalmessagequeue.sns.SnsTopicArn;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsQueueUrl;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsService;
import uk.co.nstauthority.licensingmanagementservice.authentication.logout.LogoutService;

@ExtendWith(MockitoExtension.class)
class EnergyPortalLogoutSqsServiceTest {

  private static final SqsQueueUrl QUEUE_URL = new SqsQueueUrl("queueUrl");
  private static final SnsTopicArn TOPIC_ARN = new SnsTopicArn("topicArn");

  @Mock
  private SqsService sqsService;

  @Mock
  private SnsService snsService;

  @Mock
  private LogoutService logoutService;

  private EnergyPortalLogoutSqsService energyPortalLogoutSqsService;

  @BeforeEach
  void setUp() {
    when(sqsService.getOrCreateQueue("licensing-management-service-user-sign-out"))
        .thenReturn(QUEUE_URL);

    when(snsService.getOrCreateTopic(EnergyPortalAccountsTopic.USER_SIGN_OUT.getName()))
        .thenReturn(TOPIC_ARN);

    energyPortalLogoutSqsService = new EnergyPortalLogoutSqsService(sqsService, snsService, logoutService);
  }

  @SuppressWarnings("unchecked")
  @Test
  void receiveQueueMessages() {
    var userSignOutMessage = new UserSignedOutEpasMessage(1234L, "correlation-id", Instant.now());

    doAnswer(invocation -> {
      invocation.getArgument(2, Consumer.class).accept(userSignOutMessage);
      return null;
    })
        .when(sqsService)
        .receiveQueueMessages(
            eq(QUEUE_URL),
            eq(UserSignedOutEpasMessage.class),
            any()
        );

    energyPortalLogoutSqsService.receiveQueueMessages();

    verify(logoutService).logoutUser(1234L);
  }

}