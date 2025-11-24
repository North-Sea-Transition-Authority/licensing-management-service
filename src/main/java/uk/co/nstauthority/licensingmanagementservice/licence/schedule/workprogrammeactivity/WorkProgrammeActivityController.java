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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}/work-programme-activity")
public class WorkProgrammeActivityController {

  private WorkProgrammeActivityFormService workProgrammeActivityFormService;
  private WorkProgrammeActivityFormValidator workProgrammeActivityFormValidator;
  private LicenceService licenceService;

  public WorkProgrammeActivityController(
      WorkProgrammeActivityFormService workProgrammeActivityFormService,
      WorkProgrammeActivityFormValidator workProgrammeActivityFormValidator,
      LicenceService licenceService
  ) {
    this.workProgrammeActivityFormService = workProgrammeActivityFormService;
    this.workProgrammeActivityFormValidator = workProgrammeActivityFormValidator;
    this.licenceService = licenceService;
  }

  @GetMapping("/create")
  public ModelAndView renderAddNewActivityForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getActivityModelAndView(new WorkProgrammeActivityForm(), licenceScheduleDetail);
  }

  @PostMapping("/create")
  ModelAndView submitAddNewActivityForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") WorkProgrammeActivityForm form,
      BindingResult bindingResult
  ) {
    if (!workProgrammeActivityFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getActivityModelAndView(form, licenceScheduleDetail);
    }

    workProgrammeActivityFormService.saveActivityFromForm(form, licenceScheduleDetail);

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
        .addObject("termOptions", workProgrammeActivityFormService.getScheduleTermOptions(licenceScheduleDetail))
        .addObject("phaseOptions", workProgrammeActivityFormService.getSchedulePhaseOptions(licenceScheduleDetail))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl())
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence));
  }

}
