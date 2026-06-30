package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

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
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
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
public class LicenceScheduleTermController {

  private final LicenceScheduleTermFormService licenceScheduleTermFormService;
  private final LicenceScheduleTermFormValidator licenceScheduleTermFormValidator;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceService licenceService;

  public LicenceScheduleTermController(
      LicenceScheduleTermFormService licenceScheduleTermFormService,
      LicenceScheduleTermFormValidator licenceScheduleTermFormValidator,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceService licenceService
  ) {
    this.licenceScheduleTermFormService = licenceScheduleTermFormService;
    this.licenceScheduleTermFormValidator = licenceScheduleTermFormValidator;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceService = licenceService;
  }

  @GetMapping("/{licenceScheduleDetailId}/term/create")
  public ModelAndView renderAddNewTermForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getScheduleTermModelAndView(new LicenceScheduleTermForm(), licenceScheduleDetail);
  }

  @PostMapping("/{licenceScheduleDetailId}/term/create")
  ModelAndView submitAddNewTermForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceScheduleTermForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    if (!licenceScheduleTermFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getScheduleTermModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, new LicenceScheduleTerm(), serviceUserDetail);

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  @GetMapping("/term/{licenceScheduleTermId}/update")
  public ModelAndView renderUpdateTermForm(
      @PathVariable UUID licenceScheduleTermId
  ) {
    var term = licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTermId);
    var form = licenceScheduleTermFormService.getTermForm(term);
    return getScheduleTermModelAndView(form, term.getLicenceScheduleDetail());
  }

  @PostMapping("/term/{licenceScheduleTermId}/update")
  ModelAndView submitUpdateTermForm(
      @PathVariable UUID licenceScheduleTermId,
      @ModelAttribute("form") LicenceScheduleTermForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    var term = licenceScheduleTermService.getTermByIdOrThrow(licenceScheduleTermId);
    var licenceScheduleDetail = term.getLicenceScheduleDetail();

    if (!licenceScheduleTermFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail, term)) {
      return getScheduleTermModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleTermFormService.saveTermFromForm(form, licenceScheduleDetail, term, serviceUserDetail);

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  private ModelAndView getScheduleTermModelAndView(LicenceScheduleTermForm form, LicenceScheduleDetail licenceScheduleDetail) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();

    return new ModelAndView("lms/licence/schedule/createScheduleTerm")
        .addObject("form", form)
        .addObject("radioOptions", TermType.getTermRadioOptionsFor(licence.getType()))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl())
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence));
  }
}
