package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
public class LicenceScheduleTimelineController {

  public static final String PAGE_TITLE = "%s - Licence schedule and work programme";

  private final LicenceScheduleTimelineService licenceScheduleTimelineService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public LicenceScheduleTimelineController(
      LicenceScheduleTimelineService licenceScheduleTimelineService,
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceTypeRulesResolver licenceTypeRulesResolver
  ) {
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  @GetMapping("/licence/{licenceId}/schedule")
  public ModelAndView renderLicenceScheduleTimeline(
      @PathVariable("licenceId") Integer licenceId,
      Licence licence
  ) {
    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow(
        licence,
        LicenceScheduleDetailStatus.DRAFT
    );

    return getScheduleTimelineModelAndView(licence, licenceScheduleDetail);
  }

  @GetMapping("/licence/schedule/{licenceScheduleDetailId}")
  public ModelAndView renderLicenceScheduleTimeline(
      @PathVariable("licenceScheduleDetailId") UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getScheduleTimelineModelAndView(licenceScheduleDetail.getLicenceSchedule().getLicence(), licenceScheduleDetail);
  }

  private ModelAndView getScheduleTimelineModelAndView(Licence licence, LicenceScheduleDetail licenceScheduleDetail) {
    return new ModelAndView("lms/licence/schedule/timeline/scheduleTimeline")
        .addObject("pageTitle", PAGE_TITLE.formatted(licence.getLicenceReference()))
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .addObject("actions", licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .addObject("scheduleEventViews", licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail))
        .addObject("updateLicenceStartDateUrl", ReverseRouter.route(on(LicenceStartDateController.class)
            .renderLicenceStartDateUpdateForm(licenceScheduleDetail.getId(), null))
        )
        .addObject("showRoundIssuedOn", licenceTypeRulesResolver.canShowLicenceRoundIssuedOn(licence.getType()));
  }

}
