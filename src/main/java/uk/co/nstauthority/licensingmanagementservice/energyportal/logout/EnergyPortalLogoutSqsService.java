package uk.co.nstauthority.licensingmanagementservice.energyportal.logout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.co.fivium.energyportal.accounts.epmq.EnergyPortalAccountsTopic;
import uk.co.fivium.energyportal.accounts.epmq.messages.UserSignedOutEpasMessage;
import uk.co.fivium.energyportalmessagequeue.sns.SnsService;
import uk.co.fivium.energyportalmessagequeue.sns.SnsTopicArn;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsQueueUrl;
import uk.co.fivium.energyportalmessagequeue.sqs.SqsService;
import uk.co.nstauthority.licensingmanagementservice.authentication.logout.LogoutService;

@Component
class EnergyPortalLogoutSqsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnergyPortalLogoutSqsService.class);

  private final SqsService sqsService;
  private final SnsService snsService;
  private final SqsQueueUrl sqsQueueUrl;
  private final SnsTopicArn snsTopicArn;
  private final LogoutService logoutService;

  EnergyPortalLogoutSqsService(SqsService sqsService, SnsService snsService, LogoutService logoutService) {
    this.sqsService = sqsService;
    this.snsService = snsService;
    this.sqsQueueUrl = sqsService.getOrCreateQueue("licensing-management-service-user-sign-out");
    this.snsTopicArn = snsService.getOrCreateTopic(EnergyPortalAccountsTopic.USER_SIGN_OUT.getName());
    this.logoutService = logoutService;
  }

  @EventListener(classes = ApplicationReadyEvent.class)
  void subscribeSnsTopicToEpmqQueue() {
    snsService.subscribeTopicToSqsQueue(snsTopicArn, sqsQueueUrl);
  }

  @Scheduled(fixedRate = 5000L)
  void receiveQueueMessages() {
    sqsService.receiveQueueMessages(
        sqsQueueUrl,
        UserSignedOutEpasMessage.class,
        userSignedOutEpasMessage -> {
          logoutService.logoutUser(userSignedOutEpasMessage.getWuaId());
          LOGGER.debug(
              "Logged out any sessions for user: {} due to sign out request from IDP",
              userSignedOutEpasMessage.getWuaId()
          );
        });
  }

}
