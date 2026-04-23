package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationReviewConfirmationController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationWithdrawConfirmationController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceContinuationActionItem implements Displayable {
  CONFIRM_CONTINUATION(
      "Confirm continuation",
      1,
          detail -> ReverseRouter.route(on(LicenceContinuationApplicationReviewConfirmationController.class)
                                            .renderOverviewConfirmation(detail.getId(), null))
  ),
  WITHDRAW_CONTINUATION(
      "Withdraw",
      2,
          detail -> ReverseRouter.route(on(LicenceContinuationApplicationWithdrawConfirmationController.class)
                                            .renderWithdrawConfirmation(detail.getId(), null))
  );

  private final String displayName;
  private final int displayOrder;
  private final Function<LicenceContinuationApplicationDetail, String> redirectUrl;

  LicenceContinuationActionItem(
      String displayName,
      int displayOrder,
      Function<LicenceContinuationApplicationDetail, String> redirectUrl
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
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

  public ActionItemView toActionItemView(LicenceContinuationApplicationDetail applicationDetail, boolean primaryAction) {
    return new ActionItemView(
        displayName,
        displayOrder,
        primaryAction,
        getActionRedirectUrl(applicationDetail),
        null
    );
  }

  public ActionItemView toActionItemView(LicenceContinuationApplicationDetail applicationDetail) {
    return toActionItemView(applicationDetail, false);
  }
}
