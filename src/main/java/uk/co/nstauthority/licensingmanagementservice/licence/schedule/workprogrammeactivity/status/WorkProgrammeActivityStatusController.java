package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivity;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivitySummaryView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("licence/schedule/work-programme-activity/{workProgrammeActivityId}/status")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(
        roles = {Role.WORK_PROGRAMME_ADMINISTRATOR, Role.WORK_PROGRAMME_STATUS_ADMINISTRATOR},
        teamType = TeamType.LICENCE_MANAGEMENT
    )
})
public class WorkProgrammeActivityStatusController {

  private final WorkProgrammeActivityStatusService workProgrammeActivityStatusService;
  private final WorkProgrammeActivityStatusValidator workProgrammeActivityStatusValidator;
  private final LicenceService licenceService;

  public WorkProgrammeActivityStatusController(
      WorkProgrammeActivityStatusService workProgrammeActivityStatusService,
      WorkProgrammeActivityStatusValidator workProgrammeActivityStatusValidator,
      LicenceService licenceService
  ) {
    this.workProgrammeActivityStatusService = workProgrammeActivityStatusService;
    this.workProgrammeActivityStatusValidator = workProgrammeActivityStatusValidator;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderStatusUpdatePage(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    return getStatusUpdateModelAndView(
        workProgrammeActivity,
        workProgrammeActivityStatusService.getStatusForm(workProgrammeActivity)
    );
  }

  @PostMapping
  public ModelAndView submitStatusUpdatePage(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity,
      @ModelAttribute("form") WorkProgrammeActivityStatusForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (!workProgrammeActivityStatusValidator.isValid(form, bindingResult)) {
      return getStatusUpdateModelAndView(
          workProgrammeActivity,
          form
      );
    }

    workProgrammeActivityStatusService.saveStatusFromForm(form, workProgrammeActivity);

    NotificationBanner.newSuccessBannerWithHeader(
        String.format("The status of %s has been updated", workProgrammeActivity.getCategoryString()),
        redirectAttributes
    );

    return ReverseRouter.redirect(on(LicenceOverviewController.class)
        .renderLicenceOverview(workProgrammeActivity.getLicence().getId(), null, null, null));
  }

  private ModelAndView getStatusUpdateModelAndView(
      WorkProgrammeActivity workProgrammeActivity,
      WorkProgrammeActivityStatusForm form
  ) {
    var licence = workProgrammeActivity.getLicence();

    return new ModelAndView("lms/licence/schedule/updateWorkProgrammeActivityStatus")
        .addObject("form", form)
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("summaryView", WorkProgrammeActivitySummaryView.fromWorkProgrammeActivity(workProgrammeActivity))
        .addObject("statusRadioOptions", WorkProgrammeStatus.getRadioOptions())
        .addObject("licenceSearchUrl", SearchSelectorService.route(on(LicenceInternalApiRestController.class)
            .searchLicencesByReferenceAndType(licence.getType().getUrlSlug(), null))
        )
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceOverviewController.class)
            .renderLicenceOverview(licence.getId(), null, null, null))
        );
  }

}
