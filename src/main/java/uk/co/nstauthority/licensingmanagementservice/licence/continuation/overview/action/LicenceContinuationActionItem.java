package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationReviewConfirmationController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceContinuationActionItem implements Displayable {
  CONFIRM_CONTINUATION(
      "Confirm continuation",
      1,
      true,
          detail -> ReverseRouter.route(on(LicenceContinuationApplicationReviewConfirmationController.class)
                                            .renderOverviewConfirmation(detail.getId(), null))
  );

  private final String displayName;
  private final int displayOrder;
  private final boolean primaryAction;
  private final Function<LicenceContinuationApplicationDetail, String> redirectUrl;

  LicenceContinuationActionItem(
      String displayName,
      int displayOrder,
      boolean primaryAction,
      Function<LicenceContinuationApplicationDetail, String> redirectUrl
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.primaryAction = primaryAction;
    this.redirectUrl = redirectUrl;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getActionRedirectUrl(LicenceContinuationApplicationDetail applicationDetail) {
    return redirectUrl.apply(applicationDetail);
  }

  public ActionItemView toActionItemView(LicenceContinuationApplicationDetail applicationDetail) {
    return new ActionItemView(
        displayName,
        displayOrder,
        primaryAction,
        getActionRedirectUrl(applicationDetail),
        null
    );
  }
}
