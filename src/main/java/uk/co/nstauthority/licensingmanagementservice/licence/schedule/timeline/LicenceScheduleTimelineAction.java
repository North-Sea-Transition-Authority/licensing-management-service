package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.function.Function;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry.LicenceScheduleExpiryController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate.LicenceScheduleRateController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

public enum LicenceScheduleTimelineAction {
ADD_A_TERM(
    "Add a term",
    1,
        licenceScheduleDetail -> ReverseRouter.route(on(LicenceScheduleTermController.class)
        .renderAddNewTermForm(licenceScheduleDetail.getId(), null))
),
ADD_A_PHASE(
    "Add a phase",
    2,
        licenceScheduleDetail -> ReverseRouter.route(on(LicenceSchedulePhaseController.class)
        .renderAddNewPhaseForm(licenceScheduleDetail.getId(), null))
),
ADD_A_WORK_PROGRAMME_ACTIVITY(
    "Add a work programme activity",
    3,
        licenceScheduleDetail -> ReverseRouter.route(on(WorkProgrammeActivityController.class)
        .renderAddNewActivityForm(licenceScheduleDetail.getId(), null))
),
ADD_A_RATE(
    "Add a rate",
    4,
        licenceScheduleDetail -> ReverseRouter.route(on(LicenceScheduleRateController.class)
        .renderNewLicenceScheduleRateForm(licenceScheduleDetail.getId(), null))
),
ADD_AN_EXPIRY(
  "Add an expiry",
  5,
      licenceScheduleDetail -> ReverseRouter.route(on(LicenceScheduleExpiryController.class)
        .renderAddLicenceExpiryPage(licenceScheduleDetail.getId(), null))
);

  private final String displayText;
  private final int displayOrder;
  private final Function<LicenceScheduleDetail, String> redirectUrl;

  LicenceScheduleTimelineAction(
      String displayText,
      int displayOrder,
      Function<LicenceScheduleDetail, String> redirectUrl
  ) {
    this.displayText = displayText;
    this.displayOrder = displayOrder;
    this.redirectUrl = redirectUrl;
  }

  public String getDisplayText() {
    return displayText;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getActionRedirectUrl(LicenceScheduleDetail licenceScheduleDetail) {
    return redirectUrl.apply(licenceScheduleDetail);
  }
}
