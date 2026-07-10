package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

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

@Controller
@RequestMapping("licence/schedule")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class LicenceScheduleRateController {

  private final LicenceScheduleRateFormService licenceScheduleRateFormService;
  private final LicenceScheduleRateFormValidator licenceScheduleRateFormValidator;
  private final LicenceService licenceService;
  private final LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;
  private final LicenceScheduleRateService licenceScheduleRateService;

  public LicenceScheduleRateController(
      LicenceScheduleRateFormService licenceScheduleRateFormService,
      LicenceScheduleRateFormValidator licenceScheduleRateFormValidator,
      LicenceService licenceService,
      LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService,
      LicenceScheduleRateService licenceScheduleRateService
  ) {
    this.licenceScheduleRateFormService = licenceScheduleRateFormService;
    this.licenceScheduleRateFormValidator = licenceScheduleRateFormValidator;
    this.licenceService = licenceService;
    this.licenceScheduleRelativeOptionsService = licenceScheduleRelativeOptionsService;
    this.licenceScheduleRateService = licenceScheduleRateService;
  }

  @GetMapping("/{licenceScheduleDetailId}/rates/create")
  public ModelAndView renderNewLicenceScheduleRateForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getScheduleRateModelAndView(new LicenceScheduleRateForm(), licenceScheduleDetail);
  }

  @PostMapping("/{licenceScheduleDetailId}/rates/create")
  ModelAndView submitNewLicenceScheduleRateForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceScheduleRateForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    if (!licenceScheduleRateFormValidator.isValid(form, bindingResult, licenceScheduleDetail, null)) {
      return getScheduleRateModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleRateFormService.saveRateFromForm(
        form,
        licenceScheduleDetail,
        new LicenceScheduleRate(),
        serviceUserDetail
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  @GetMapping("/rate/{licenceScheduleRateId}/update")
  public ModelAndView renderUpdateLicenceScheduleRateForm(
      @PathVariable UUID licenceScheduleRateId
  ) {
    var rate = licenceScheduleRateService.getRateByIdOrThrow(licenceScheduleRateId);

    return getScheduleRateModelAndView(
        licenceScheduleRateFormService.getFormFromRate(rate),
        rate.getLicenceScheduleDetail()
    );
  }

  @PostMapping("/rate/{licenceScheduleRateId}/update")
  ModelAndView submitUpdateLicenceScheduleRateForm(
      @PathVariable UUID licenceScheduleRateId,
      @ModelAttribute("form") LicenceScheduleRateForm form,
      BindingResult bindingResult,
      ServiceUserDetail serviceUserDetail
  ) {
    var rate = licenceScheduleRateService.getRateByIdOrThrow(licenceScheduleRateId);
    var detail = rate.getLicenceScheduleDetail();

    if (!licenceScheduleRateFormValidator.isValid(form, bindingResult, detail, rate)) {
      return getScheduleRateModelAndView(form, detail);
    }

    licenceScheduleRateFormService.saveRateFromForm(
        form,
        detail,
        rate,
        serviceUserDetail
    );

    return detail.getScheduleTimelineRedirectUrl();
  }

  private ModelAndView getScheduleRateModelAndView(LicenceScheduleRateForm form, LicenceScheduleDetail licenceScheduleDetail) {
    return new ModelAndView("lms/licence/schedule/createScheduleRate")
        .addObject("form", form)
        .addObject("termOptions", licenceScheduleRelativeOptionsService.getScheduleTermOptions(licenceScheduleDetail))
        .addObject("phaseOptions", licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail))
        .addObject("rateDefinitionOptions", licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail))
        .addObject("relativeEventOptions", licenceScheduleRelativeOptionsService.getRelativeEventOptions(licenceScheduleDetail))
        .addObject("relativeDateOptions", RateRelativeDateOption.getRateRelativeDateOptions())
        .addObject("pageCaption", licenceService.getLicencePageCaption(licenceScheduleDetail.getLicenceSchedule().getLicence()))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl());
  }
}
