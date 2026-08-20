package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LicenceActionEndPointInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceScheduleTabController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.action.LicenceActionItem;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/{licenceId}/schedule/new-draft/")
@LicenceActionEndPointInterceptorRule.ActionEndPoint(LicenceActionItem.UPDATE_LICENCE_SCHEDULE)
public class LicenceScheduleDetailDuplicationController {

  private final LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceService licenceService;

  public LicenceScheduleDetailDuplicationController(
      LicenceScheduleDetailDuplicationService licenceScheduleDetailDuplicationService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceService licenceService
  ) {
    this.licenceScheduleDetailDuplicationService = licenceScheduleDetailDuplicationService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderCreateDraftScheduleUpdatePage(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    return new ModelAndView("lms/licence/schedule/startScheduleUpdateJourney")
        .addObject("pageTitle", "Update an existing licence schedule")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("startUrl",
            ReverseRouter.route(on(LicenceScheduleDetailDuplicationController.class)
                .createDraftScheduleUpdateAndRedirect(licenceId, null))
        )
        .addObject("backUrl",
            ReverseRouter.route(on(LicenceScheduleTabController.class)
                .renderLicenceOverview(licenceId, null, null, null))
        );
  }

  @PostMapping()
  ModelAndView createDraftScheduleUpdateAndRedirect(
      @PathVariable Integer licenceId,
      Licence licence
  ) {
    var oldDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.ACTIVE
    );

    var newDetail = licenceScheduleDetailDuplicationService.createNewDraftLicenceScheduleDetailVersion(oldDetail);

    return ReverseRouter.redirect(on(LicenceScheduleTimelineController.class).renderLicenceScheduleTimeline(
        newDetail.getId(),
        null,
        null,
        null
    ));
  }

}
