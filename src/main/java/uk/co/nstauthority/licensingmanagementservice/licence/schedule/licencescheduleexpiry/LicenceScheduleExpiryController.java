package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleexpiry;

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
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("licence/schedule/{licenceScheduleDetailId}/expiry/create")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.SCHEDULE_ADMINISTRATOR}, teamType = TeamType.LICENCE_MANAGEMENT)
})
@LicenceScheduleDetailHasStatus(value = LicenceScheduleDetailStatus.DRAFT)
public class LicenceScheduleExpiryController {

  private final LicenceScheduleExpiryService licenceScheduleExpiryService;
  private final LicenceScheduleExpiryFormValidator licenceScheduleExpiryFormValidator;
  private final LicenceService licenceService;

  public LicenceScheduleExpiryController(
      LicenceScheduleExpiryService licenceScheduleExpiryService,
      LicenceScheduleExpiryFormValidator licenceScheduleExpiryFormValidator,
      LicenceService licenceService
  ) {
    this.licenceScheduleExpiryService = licenceScheduleExpiryService;
    this.licenceScheduleExpiryFormValidator = licenceScheduleExpiryFormValidator;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderAddUpdateLicenceExpiryPage(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    var expiry = licenceScheduleExpiryService.getOrCreateExpiry(licenceScheduleDetail);

    return getLicenceExpiryModelAndView(licenceScheduleExpiryService.getExpiryForm(expiry), licenceScheduleDetail);
  }

  @PostMapping
  ModelAndView submitAddUpdateLicenceExpiryPage(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceScheduleExpiryForm form,
      BindingResult bindingResult
  ) {
    var expiry = licenceScheduleExpiryService.getOrCreateExpiry(licenceScheduleDetail);

    if (!licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getLicenceExpiryModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleExpiryService.saveExpiryFromForm(
        form,
        licenceScheduleDetail,
        expiry
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  private ModelAndView getLicenceExpiryModelAndView(
      LicenceScheduleExpiryForm form,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return new ModelAndView("lms/licence/schedule/createLicenceExpiry")
        .addObject("form", form)
        .addObject("pageCaption", licenceService.getLicencePageCaption(licenceScheduleDetail.getLicenceSchedule().getLicence()))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl());
  }
}
