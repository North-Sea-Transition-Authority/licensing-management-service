package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.start.StartLicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailDuplicationController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney.StartLicenceScheduleJourneyController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceActionItem implements Displayable {
  CREATE_LICENCE_SCHEDULE(
      "Create licence schedule",
      1,
          licence -> ReverseRouter.route(on(StartLicenceScheduleJourneyController.class)
              .renderStartLicenceScheduleJourney(licence.getId(), null))
  ),
  UPDATE_LICENCE_SCHEDULE(
      "Update licence schedule",
      2,
          licence -> ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class)
          .renderCreateDraftScheduleUpdatePage(licence.getId(), null))
  ),
  EDIT_LICENCE_DETAILS(
      "Edit licence details",
          3,
          licence -> ReverseRouter.route(on(LicenceController.class)
              .renderEditLicenceDetailsPage(licence.getId(), null))
  ),
  START_CORRECTION(
      "Start licence correction",
      4,
          licence -> ReverseRouter.route(on(StartLicenceCorrectionController.class).renderStartLicenceCorrection(licence))
  )
  ;

  private final String displayName;
  private final int displayOrder;
  private final Function<Licence, String> redirectUrl;

  LicenceActionItem(
      String displayName,
      int displayOrder,
      Function<Licence, String> redirectUrl
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

  public String getActionRedirectUrl(Licence licence) {
    return redirectUrl.apply(licence);
  }

  public ActionItemView toActionItemView(Licence licence, boolean primaryAction) {
    return new ActionItemView(
        displayName,
        displayOrder,
        primaryAction,
        getActionRedirectUrl(licence),
        null
    );
  }

  public ActionItemView toActionItemView(Licence licence) {
    return toActionItemView(licence, false);
  }
}
