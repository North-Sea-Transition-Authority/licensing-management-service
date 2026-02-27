package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward.AllocateStewardController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ScheduleWorkProgrammeApplicationActionItem implements Displayable {
  ALLOCATE_STEWARD(
      "Allocate steward",
      1,
      false,
          detail -> ReverseRouter.route(on(AllocateStewardController.class).render(detail.getId(), null))
  );

  private final String displayName;
  private final int displayOrder;
  private final boolean primaryAction;
  private final Function<ScheduleWorkProgrammeApplicationDetail, String> redirectUrl;

  ScheduleWorkProgrammeApplicationActionItem(
      String displayName,
      int displayOrder,
      boolean primaryAction,
      Function<ScheduleWorkProgrammeApplicationDetail, String> redirectUrl
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

  public String getActionRedirectUrl(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return redirectUrl.apply(applicationDetail);
  }

  public ActionItemView toActionItemView(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    return new ActionItemView(
        displayName,
        displayOrder,
        primaryAction,
        getActionRedirectUrl(applicationDetail),
        null
    );
  }
}
