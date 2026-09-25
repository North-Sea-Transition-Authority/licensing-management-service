package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

public class LmsAbsoluteUrlUtil {

  private LmsAbsoluteUrlUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static String getWorkAreaUrl(String baseUrl) {
    var workAreaUrl = ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null));
    return "%s%s".formatted(baseUrl, workAreaUrl);
  }

  public static String getAbsoluteUrl(String relativeUrl) {
    return "%s%s".formatted(getBaseUrl(), relativeUrl);
  }

  /** For use outside the current request's MVC handling (e.g. security filters), where there's no ambient request context. */
  public static String getWorkAreaUrl(HttpServletRequest request) {
    var workAreaUrl = ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null));
    return getAbsoluteUrl(workAreaUrl, request);
  }

  public static String getAbsoluteUrl(String relativeUrl, HttpServletRequest request) {
    return "%s%s".formatted(getBaseUrl(request), relativeUrl);
  }

  private static String getBaseUrl(HttpServletRequest request) {
    return ServletUriComponentsBuilder.fromContextPath(request).toUriString();
  }

  private static String getBaseUrl() {
    return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
  }
}
