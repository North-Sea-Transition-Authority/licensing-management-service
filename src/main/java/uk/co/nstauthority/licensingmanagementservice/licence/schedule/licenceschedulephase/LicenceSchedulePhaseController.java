package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.licencescheduledetail.LicenceScheduleDetailHasStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;


@Controller
@RequestMapping("/licence/schedule")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class LicenceSchedulePhaseController {

  private final LicenceSchedulePhaseFormService licenceSchedulePhaseFormService;
  private final LicenceSchedulePhaseFormValidator licenceSchedulePhaseFormValidator;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceService licenceService;

  public LicenceSchedulePhaseController(
      LicenceSchedulePhaseFormService licenceSchedulePhaseFormService,
      LicenceSchedulePhaseFormValidator licenceSchedulePhaseFormValidator,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceService licenceService
  ) {
    this.licenceSchedulePhaseFormService = licenceSchedulePhaseFormService;
    this.licenceSchedulePhaseFormValidator = licenceSchedulePhaseFormValidator;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceService = licenceService;
  }

  @GetMapping("/{licenceScheduleDetailId}/phase/create")
  public ModelAndView renderAddNewPhaseForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getSchedulePhaseModelAndView(new LicenceSchedulePhaseForm(), licenceScheduleDetail);
  }

  @PostMapping("/{licenceScheduleDetailId}/phase/create")
  ModelAndView submitAddNewPhaseForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceSchedulePhaseForm form,
      BindingResult bindingResult
  ) {
    if (!licenceSchedulePhaseFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getSchedulePhaseModelAndView(form, licenceScheduleDetail);
    }

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail, new LicenceSchedulePhase());

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  @GetMapping("/phase/{licenceSchedulePhaseId}/update")
  public ModelAndView renderUpdatePhaseForm(
      @PathVariable UUID licenceSchedulePhaseId
  ) {
    var phase = licenceSchedulePhaseService.getPhaseByIdOrThrow(licenceSchedulePhaseId);
    var form = licenceSchedulePhaseFormService.getPhaseForm(phase);
    return getSchedulePhaseModelAndView(form, phase.getLicenceScheduleDetail());
  }

  @PostMapping("/phase/{licenceSchedulePhaseId}/update")
  ModelAndView submitUpdatePhaseForm(
      @PathVariable UUID licenceSchedulePhaseId,
      @ModelAttribute("form") LicenceSchedulePhaseForm form,
      BindingResult bindingResult
  ) {
    var phase = licenceSchedulePhaseService.getPhaseByIdOrThrow(licenceSchedulePhaseId);
    var licenceScheduleDetail = phase.getLicenceScheduleDetail();

    if (!licenceSchedulePhaseFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, phase)) {
      return getSchedulePhaseModelAndView(form, licenceScheduleDetail);
    }

    licenceSchedulePhaseFormService.savePhaseFromForm(form, licenceScheduleDetail, phase);

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }
  
  private ModelAndView getSchedulePhaseModelAndView(LicenceSchedulePhaseForm form, LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/createSchedulePhase")
        .addObject("form", form)
        .addObject("radioOptions", PhaseType.getPhaseRadioOptionsFor(licence.getType()))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl())
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence));
  }

}
