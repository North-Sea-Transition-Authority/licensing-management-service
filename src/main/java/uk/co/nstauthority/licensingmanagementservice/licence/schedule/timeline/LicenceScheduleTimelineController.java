package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/{licenceId}/schedule")
public class LicenceScheduleTimelineController {

  public static final String PAGE_TITLE = "%s - Licence schedule and work programme";

  private final LicenceScheduleTimelineService licenceScheduleTimelineService;
  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public LicenceScheduleTimelineController(
      LicenceScheduleTimelineService licenceScheduleTimelineService,
      LicenceScheduleDetailService licenceScheduleDetailService
  ) {
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  @GetMapping
  public ModelAndView renderLicenceScheduleTimeline(
      @PathVariable("licenceId") Integer licenceId,
      Licence licence
  ) {
    var licenceScheduleDetail = licenceScheduleDetailService.getScheduleDetailByLicenceOrThrow(licence);

    return new ModelAndView("lms/licence/schedule/scheduleTimeline")
        .addObject("pageTitle", PAGE_TITLE.formatted(licence.getLicenceReference()))
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .addObject("actions", licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .addObject("scheduleEventViews", licenceScheduleTimelineService.getLicenceScheduleEventViews(licenceScheduleDetail))
        .addObject("updateLicenceStartDateUrl", ReverseRouter.route(on(LicenceStartDateController.class)
                .renderLicenceStartDateUpdateForm(licenceScheduleDetail.getId(), null))
        );
  }

}
