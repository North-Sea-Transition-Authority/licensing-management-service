package uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A utility class for creating and adding <a href="https://design-system.fivium.co.uk/components/notification-banner">notification banners</a> to pages. For example:
 *
 * <pre>
 *
 *   ModelAndView getModelAndView(RedirectAttributes redirectAttributes) {
 *     NotificationBanner.newSuccessBanner()
 *         .withHeadingContent("This is a green success banner")
 *         .applyTo(redirectAttributes); // you can also directly to the model and view
 *
 *     return new ModelAndView("templates/example-page");
 *   }
 *
 * </pre>
 *
 */
public record NotificationBanner(
    String title,
    String headingContent,
    String otherContent,
    NotificationBannerType type
) implements Serializable {

  @Serial
  private static final long serialVersionUID = -993623327229159237L;

  private static final String NOTIFICATION_BANNER = "notificationBanner";

  public static Builder newSuccessBanner() {
    return new Builder(NotificationBannerType.SUCCESS, "Success");
  }

  public static Builder newInfoBanner() {
    return new Builder(NotificationBannerType.INFO, "Information");
  }

  public static class Builder {

    private final String title;
    private final NotificationBannerType notificationBannerType;

    private String headingContent;
    private String otherContent;

    private Builder(NotificationBannerType notificationBannerType, String title) {
      this.notificationBannerType = notificationBannerType;
      this.title = title;
    }

    public Builder withHeadingContent(String headingContent) {
      this.headingContent = headingContent;
      return this;
    }

    public Builder withOtherContent(String otherContent) {
      this.otherContent = otherContent;
      return this;
    }

    public NotificationBanner build() {
      Objects.requireNonNull(title, "title must not be null");
      return new NotificationBanner(title, headingContent, otherContent, notificationBannerType);
    }

    public void applyTo(ModelAndView modelAndView) {
      modelAndView.addObject(NOTIFICATION_BANNER, this.build());
    }

    public void applyTo(RedirectAttributes redirectAttributes) {
      redirectAttributes.addFlashAttribute(NOTIFICATION_BANNER, this.build());
    }
  }
}
