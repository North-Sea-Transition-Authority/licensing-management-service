package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LicenceActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licence/{licenceId}/schedule/start")
@LicenceActionEndPointInterceptorRule.ActionEndPoint(LicenceActionItem.CREATE_LICENCE_SCHEDULE)
public class StartLicenceScheduleJourneyController {

  private final LicenceService licenceService;

  public StartLicenceScheduleJourneyController(LicenceService licenceService) {
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderStartLicenceScheduleJourney(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    return new ModelAndView("lms/licence/schedule/startScheduleJourney")
        .addObject("pageTitle", "Create a new licence schedule")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("startUrl",
            ReverseRouter.route(on(LicenceStartDateController.class).renderLicenceStartDateForm(licenceId, null)))
        .addObject("backUrl",
            ReverseRouter.route(on(LicenceOverviewController.class).renderLicenceOverview(licenceId, null, null, null)));
  }

}
