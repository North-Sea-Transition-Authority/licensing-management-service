package uk.co.nstauthority.licensingmanagementservice.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.fivium.feedbackmanagementservice.client.CannotSendFeedbackException;
import uk.co.fivium.feedbackmanagementservice.client.FeedbackClientService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

  private static final String CONTEXT_PATH = "/service-name";
  private static final Instant CURRENT_INSTANT = Instant.now();

  private static final ServiceUserDetail USER_DETAIL_WITH_PROXY = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(((long) ThreadLocalRandom.current().nextInt()))
      .build();

  private static final ServiceUserDetail USER_DETAIL_WITHOUT_PROXY = ServiceUserDetailTestUtil.newBuilder()
      .withWuaId(((long) ThreadLocalRandom.current().nextInt()))
      .buildWithoutProxy();

  @Mock
  private Clock clock;

  @Mock
  private FeedbackClientService feedbackClientService;

  @InjectMocks
  private FeedbackService feedbackService;

  @Captor
  private ArgumentCaptor<Feedback> feedbackArgumentCaptor;

  private FeedbackForm form;

  @BeforeAll
  static void setUp() {
    var request = new MockHttpServletRequest();
    request.setContextPath(CONTEXT_PATH);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @BeforeEach
  void setup() {
    form = new FeedbackForm();
    form.setServiceRating(ServiceFeedbackRating.SATISFIED.name());
    form.getFeedback().setInputValue("feedback test");

    when(clock.instant()).thenReturn(CURRENT_INSTANT);
  }

  @ParameterizedTest
  @MethodSource("getFeedbackUsers")
  void saveFeedback_cannotSendFeedbackException(ServiceUserDetail feedbackUser) throws CannotSendFeedbackException {
    when(feedbackClientService.saveFeedback(any(Feedback.class)))
        .thenThrow(new CannotSendFeedbackException("test exception"));

    feedbackService.saveFeedback(form.getServiceRating(), form.getFeedback().getInputValue(), feedbackUser);

    verify(feedbackClientService).saveFeedback(feedbackArgumentCaptor.capture());

    assertThat(feedbackArgumentCaptor.getValue()).extracting(
        Feedback::getSubmitterName,
        Feedback::getSubmitterEmail,
        Feedback::getServiceRating,
        Feedback::getComment,
        Feedback::getGivenDatetime,
        Feedback::getTransactionId,
        Feedback::getTransactionReference,
        Feedback::getTransactionLink
    ).containsExactly(
        feedbackUser.displayNameIncludingAnyProxyUser(),
        feedbackUser.emailAddress(),
        form.getServiceRating(),
        form.getFeedback().getInputValue(),
        CURRENT_INSTANT,
        null,
        null,
        null
    );
  }

  @ParameterizedTest
  @MethodSource("getFeedbackUsers")
  void saveFeedback(ServiceUserDetail feedbackUser) throws CannotSendFeedbackException {
    assertDoesNotThrow(() -> feedbackService.saveFeedback(
        form.getServiceRating(),
        form.getFeedback().getInputValue(),
        feedbackUser)
    );

    verify(feedbackClientService).saveFeedback(feedbackArgumentCaptor.capture());

    assertThat(feedbackArgumentCaptor.getValue()).extracting(
        Feedback::getSubmitterName,
        Feedback::getSubmitterEmail,
        Feedback::getServiceRating,
        Feedback::getComment,
        Feedback::getGivenDatetime,
        Feedback::getTransactionId,
        Feedback::getTransactionReference,
        Feedback::getTransactionLink
    ).containsExactly(
        feedbackUser.displayNameIncludingAnyProxyUser(),
        feedbackUser.emailAddress(),
        form.getServiceRating(),
        form.getFeedback().getInputValue(),
        CURRENT_INSTANT,
        null,
        null,
        null
    );
  }

  private static Stream<Arguments> getFeedbackUsers() {
    return Stream.of(
        Arguments.of(USER_DETAIL_WITH_PROXY),
        Arguments.of(USER_DETAIL_WITHOUT_PROXY)
    );
  }
}
