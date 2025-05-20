package uk.co.nstauthority.template.xyzapplication.processing.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.workarea.WorkAreaController;
import uk.co.nstauthority.template.xyzapplication.XyzApplication;

public enum CaseProcessingActionItem {

  PROGRESS_APPLICATION(
      "Progress application",
          application -> ReverseRouter.route(on(WorkAreaController.class)
          .getWorkArea(null, null))
  ),
  VERIFY_APPLICATION(
      "Verify application",
          application -> ReverseRouter.route(on(WorkAreaController.class)
          .getWorkArea(null, null))
  );

  private final String displayName;
  private final Function<XyzApplication, String> redirectUrl;

  CaseProcessingActionItem(
      String displayName,
      Function<XyzApplication, String> redirectUrl
  ) {
    this.displayName = displayName;
    this.redirectUrl = redirectUrl;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getActionRedirectUrl(XyzApplication xyzApplication) {
    return redirectUrl == null ? null : redirectUrl.apply(xyzApplication);
  }
}
