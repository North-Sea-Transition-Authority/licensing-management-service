package uk.co.nstauthority.template.feedback;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.feedbackmanagementservice.client.CannotSendFeedbackException;
import uk.co.fivium.feedbackmanagementservice.client.FeedbackClientService;
import uk.co.nstauthority.template.authentication.ServiceUserDetail;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;

@Service
class FeedbackService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackService.class);

  private final Clock clock;
  private final FeedbackClientService feedbackClientService;

  FeedbackService(Clock clock,
                  FeedbackClientService feedbackClientService
  ) {
    this.clock = clock;
    this.feedbackClientService = feedbackClientService;
  }

  void saveFeedback(String serviceRating,
                    String feedbackText,
                    ServiceUserDetail userDetail) {
    var feedback = new Feedback();
    feedback.setServiceRating(serviceRating);
    feedback.setComment(feedbackText);
    feedback.setGivenDatetime(clock.instant());
    feedback.setSubmitterEmail(userDetail.emailAddress());
    feedback.setSubmitterName(userDetail.displayNameIncludingAnyProxyUser());
    sendFeedback(feedback);
  }

  void saveFeedback(XyzApplication xyzApplication,
                    String serviceRating,
                    String feedbackText,
                    ServiceUserDetail userDetail) {
    var feedback = new Feedback();
    feedback.setTransactionId(xyzApplication.getId().toString());
    // XYZ Mocked up reference
    feedback.setTransactionReference(xyzApplication.getReference());
    feedback.setServiceRating(serviceRating);
    feedback.setComment(feedbackText);
    feedback.setGivenDatetime(clock.instant());
    feedback.setSubmitterEmail(userDetail.emailAddress());
    feedback.setSubmitterName(userDetail.displayNameIncludingAnyProxyUser());
    sendFeedback(feedback);
  }

  void sendFeedback(Feedback feedback) {
    try {
      feedbackClientService.saveFeedback(feedback);
    } catch (CannotSendFeedbackException e) {
      LOGGER.error("Feedback failed to send: ", e);
    }
  }
}
