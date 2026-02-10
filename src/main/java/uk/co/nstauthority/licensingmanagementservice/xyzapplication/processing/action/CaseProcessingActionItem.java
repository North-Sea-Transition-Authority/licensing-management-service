package uk.co.nstauthority.licensingmanagementservice.xyzapplication.processing.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;
import uk.co.nstauthority.licensingmanagementservice.xyzapplication.XyzApplication;

public enum CaseProcessingActionItem implements Displayable {

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

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public String getActionRedirectUrl(XyzApplication xyzApplication) {
    return redirectUrl == null ? null : redirectUrl.apply(xyzApplication);
  }
}
