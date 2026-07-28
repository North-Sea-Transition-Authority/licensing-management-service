package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.co.nstauthority.licensingmanagementservice.util.IllegalUtilClassInstantiationException;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

public class LmsAbsoluteUrlUtil {

  private LmsAbsoluteUrlUtil() {
    throw new IllegalUtilClassInstantiationException(this.getClass());
  }

  public static String getWorkAreaUrl() {
    var workAreaUrl = ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null));
    return "%s%s".formatted(getBaseUrl(), workAreaUrl);
  }

  private static String getBaseUrl() {
    return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
  }
}
