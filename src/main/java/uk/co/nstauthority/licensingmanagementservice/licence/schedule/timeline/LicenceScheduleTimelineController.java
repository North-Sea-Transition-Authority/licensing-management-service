package uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}")
public class LicenceScheduleTimelineController {

  public static final String PAGE_TITLE = "%s - Licence schedule and work programme";

  private final LicenceScheduleTimelineService licenceScheduleTimelineService;

  public LicenceScheduleTimelineController(LicenceScheduleTimelineService licenceScheduleTimelineService) {
    this.licenceScheduleTimelineService = licenceScheduleTimelineService;
  }

  @GetMapping
  public ModelAndView renderLicenceScheduleTimeline(
      @PathVariable("licenceScheduleDetailId") UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var licenceRef = licenceScheduleDetail.getLicenceSchedule().getLicence().getLicenceReference();

    return new ModelAndView("lms/licence/schedule/scheduleTimeline")
        .addObject("pageTitle", PAGE_TITLE.formatted(licenceRef))
        .addObject("timelineSummaryCardView", licenceScheduleTimelineService.getTimelineSummaryCardView(licenceScheduleDetail))
        .addObject("actions", licenceScheduleTimelineService.getLicenceScheduleTimelineActions(licenceScheduleDetail))
        .addObject("updateLicenceStartDateUrl", ReverseRouter.route(on(LicenceStartDateController.class)
                .renderLicenceStartDateUpdateForm(licenceScheduleDetailId, null))
        );
  }

}
