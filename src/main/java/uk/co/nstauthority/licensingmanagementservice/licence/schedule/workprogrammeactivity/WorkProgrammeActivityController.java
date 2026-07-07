package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail.LicenceScheduleDetailHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence/schedule")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.WORK_PROGRAMME_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class WorkProgrammeActivityController {

  private final WorkProgrammeActivityFormService workProgrammeActivityFormService;
  private final WorkProgrammeActivityFormValidator workProgrammeActivityFormValidator;
  private final LicenceService licenceService;
  private final LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;

  public WorkProgrammeActivityController(
      WorkProgrammeActivityFormService workProgrammeActivityFormService,
      WorkProgrammeActivityFormValidator workProgrammeActivityFormValidator,
      LicenceService licenceService,
      LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService
  ) {
    this.workProgrammeActivityFormService = workProgrammeActivityFormService;
    this.workProgrammeActivityFormValidator = workProgrammeActivityFormValidator;
    this.licenceService = licenceService;
    this.licenceScheduleRelativeOptionsService = licenceScheduleRelativeOptionsService;
  }

  @GetMapping("/{licenceScheduleDetailId}/work-programme-activity/create")
  public ModelAndView renderAddNewActivityForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getActivityModelAndView(new WorkProgrammeActivityForm(), licenceScheduleDetail);
  }

  @PostMapping("/{licenceScheduleDetailId}/work-programme-activity/create")
  ModelAndView submitAddNewActivityForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") WorkProgrammeActivityForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    if (!workProgrammeActivityFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getActivityModelAndView(form, licenceScheduleDetail);
    }

    workProgrammeActivityFormService.saveActivityFromForm(
        form,
        licenceScheduleDetail,
        new WorkProgrammeActivity(),
        serviceUserDetail
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  @GetMapping("work-programme-activity/{workProgrammeActivityId}/update")
  public ModelAndView renderUpdateActivityForm(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity
  ) {
    return getActivityModelAndView(
        workProgrammeActivityFormService.getActivityForm(workProgrammeActivity),
        workProgrammeActivity.getLicenceScheduleDetail()
    );
  }

  @PostMapping("work-programme-activity/{workProgrammeActivityId}/update")
  ModelAndView submitUpdateActivityForm(
      @PathVariable UUID workProgrammeActivityId,
      WorkProgrammeActivity workProgrammeActivity,
      @ModelAttribute("form") WorkProgrammeActivityForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    var licenceScheduleDetail = workProgrammeActivity.getLicenceScheduleDetail();

    if (!workProgrammeActivityFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getActivityModelAndView(form, licenceScheduleDetail);
    }
    workProgrammeActivityFormService.saveActivityFromForm(
        form,
        licenceScheduleDetail,
        workProgrammeActivity,
        serviceUserDetail
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  private ModelAndView getActivityModelAndView(WorkProgrammeActivityForm form, LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/createWorkProgrammeActivity")
        .addObject("form", form)
        .addObject("categoryRadioOptions", WorkProgrammeActivityCategory.getCategoriesForLicenceType(licence.getType()))
        .addObject("commitmentRadioOptions",
            DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeActivityCommitment.class))
        .addObject("activityDateRadioOptions", workProgrammeActivityFormService.getDateOptions(licenceScheduleDetail))
        .addObject("termOptions", licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail))
        .addObject("phaseOptions", licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail))
        .addObject("relativeOptions", licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl())
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence));
  }

}
