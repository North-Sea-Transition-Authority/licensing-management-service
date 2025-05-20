package uk.co.nstauthority.template.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.web.servlet.ResultMatcher;
import uk.co.nstauthority.template.fds.notificationbanner.NotificationBanner;

public class NotificationBannerTestUtil {

  private static final String NOTIFICATION_BANNER = "notificationBanner";

  private NotificationBannerTestUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static ResultMatcher notificationBanner(NotificationBanner notificationBanner) {
    return result -> {
      var actualBanner = (NotificationBanner) result.getFlashMap().get(NOTIFICATION_BANNER);
      assertThat(actualBanner).isEqualTo(notificationBanner);
    };
  }

  public static ResultMatcher notificationBannerDoesNotExist() {
    return result -> {
      var flashMap = result.getFlashMap();
      assertThat(flashMap.containsKey(NOTIFICATION_BANNER)).isFalse();
    };
  }

}
