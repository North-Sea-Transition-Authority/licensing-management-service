package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail.LicenceScheduleDetailHasStatus;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/licence/schedule/rate/{licenceScheduleRateId}/delete")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class LicenceScheduleRateDeletionController {

  private static final String PAGE_TITLE = "Do you want to delete this rate?";

  private final LicenceScheduleRateService licenceScheduleRateService;
  private final LicenceService licenceService;

  public LicenceScheduleRateDeletionController(
      LicenceScheduleRateService licenceScheduleRateService,
      LicenceService licenceService
  ) {
    this.licenceScheduleRateService = licenceScheduleRateService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderDeleteRatePage(
      @PathVariable UUID licenceScheduleRateId
  ) {
    var rate = licenceScheduleRateService.getRateByIdOrThrow(licenceScheduleRateId);

    return new ModelAndView("lms/licence/schedule/deleteScheduleRate")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("summaryView", LicenceScheduleRateSummaryView.from(rate))
        .addObject("cancelUrl", rate.getLicenceScheduleDetail().getScheduleTimelineRouteUrl())
        .addObject("pageCaption",
            licenceService.getLicencePageCaption(rate.getLicenceScheduleDetail().getLicenceSchedule().getLicence()));
  }

  @PostMapping
  ModelAndView submitDeleteRatePage(
      @PathVariable UUID licenceScheduleRateId,
      RedirectAttributes redirectAttributes
  ) {
    var rate = licenceScheduleRateService.getRateByIdOrThrow(licenceScheduleRateId);

    var licenceScheduleDetail = rate.getLicenceScheduleDetail();
    licenceScheduleRateService.deleteLicenceScheduleRate(rate);

    NotificationBanner.newSuccessBannerWithHeader(
        "The rate has been deleted",
        redirectAttributes
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }
}
