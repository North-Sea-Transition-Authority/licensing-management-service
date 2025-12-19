package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;

@Controller
@RequestMapping("/licence/schedule/work-programme-activity/{workProgrammeActivityId}/delete")
public class WorkProgrammeActivityDeletionController {

  private static final String PAGE_TITLE = "Do you want to delete the %s activity?";

  private final WorkProgrammeActivityService workProgrammeActivityService;
  private final LicenceService licenceService;

  public WorkProgrammeActivityDeletionController(
      WorkProgrammeActivityService workProgrammeActivityService,
      LicenceService licenceService
  ) {
    this.workProgrammeActivityService = workProgrammeActivityService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderDeleteActivityPage(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity activity
  ) {
    return new ModelAndView("lms/licence/schedule/deleteWorkProgrammeActivity")
        .addObject("pageTitle", PAGE_TITLE.formatted(activity.getCategoryString()))
        .addObject("summaryView", WorkProgrammeActivitySummaryView.fromWorkProgrammeActivity(activity))
        .addObject("cancelUrl", activity.getLicenceScheduleDetail().getScheduleTimelineRouteUrl())
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(activity.getLicenceScheduleDetail().getLicenceSchedule().getLicence()));
  }

  @PostMapping
  ModelAndView submitDeleteActivityPage(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity activity,
      RedirectAttributes redirectAttributes
  ) {
    var licenceScheduleDetail = activity.getLicenceScheduleDetail();
    workProgrammeActivityService.deleteWorkProgrammeActivity(activity);

    NotificationBanner.newSuccessBannerWithHeader(
        String.format("%s has been deleted", activity.getCategoryString()), redirectAttributes
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

}
