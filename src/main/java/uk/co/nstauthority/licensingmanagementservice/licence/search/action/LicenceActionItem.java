package uk.co.nstauthority.licensingmanagementservice.licence.search.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.StartLicenceScheduleJourneyController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceActionItem implements Displayable {
  CREATE_LICENCE_SCHEDULE(
      "Create licence schedule",
      1,
      false,
          licence -> ReverseRouter.route(on(StartLicenceScheduleJourneyController.class)
              .renderStartLicenceScheduleJourney(licence.getId(), null))
  ),
  EDIT_LICENCE_SCHEDULE(
      "Edit licence schedule",
      1,
      false,
          licence -> ReverseRouter.route(on(LicenceScheduleTimelineController.class)
              .renderLicenceScheduleTimeline(licence.getId(), null))
  ),
  MANAGE_LICENSEES(
      "Manage licensees",
          2,
      false,
          licence -> ReverseRouter.route(on(LicenceController.class)
              .renderManageLicenseesPage(licence.getId(), null))
  );

  private final String displayName;
  private final int displayOrder;
  private final boolean primaryAction;
  private final Function<Licence, String> redirectUrl;

  LicenceActionItem(
      String displayName,
      int displayOrder,
      boolean primaryAction,
      Function<Licence, String> redirectUrl
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

  public String getActionRedirectUrl(Licence licence) {
    return redirectUrl.apply(licence);
  }

  public ActionItemView toActionItemView(Licence licence) {
    return new ActionItemView(
        displayName,
        displayOrder,
        primaryAction,
        getActionRedirectUrl(licence),
        null
    );
  }

}
