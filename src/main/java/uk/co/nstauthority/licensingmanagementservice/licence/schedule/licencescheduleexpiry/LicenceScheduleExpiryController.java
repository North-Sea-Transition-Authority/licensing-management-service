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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Controller
@RequestMapping("licence/schedule")
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

  @GetMapping("/{licenceScheduleDetailId}/expiry/create")
  public ModelAndView renderAddLicenceExpiryPage(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getLicenceExpiryModelAndView(new LicenceScheduleExpiryForm(), licenceScheduleDetail);
  }

  @PostMapping("/{licenceScheduleDetailId}/expiry/create")
  ModelAndView submitAddLicenceExpiryPage(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceScheduleExpiryForm form,
      BindingResult bindingResult
  ) {
    if (!licenceScheduleExpiryFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getLicenceExpiryModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleExpiryService.saveExpiryFromForm(
        form,
        licenceScheduleDetail,
        new LicenceScheduleExpiry()
    );

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  @GetMapping("/expiry/{licenceScheduleExpiryId}/update")
  public ModelAndView renderUpdateLicenceExpiryPage(
      @PathVariable UUID licenceScheduleExpiryId
  ) {
    var expiry = licenceScheduleExpiryService.getExpiryByIdOrThrow(licenceScheduleExpiryId);

    return getLicenceExpiryModelAndView(
        licenceScheduleExpiryService.getExpiryForm(expiry),
        expiry.getLicenceScheduleDetail()
    );
  }

  @PostMapping("/expiry/{licenceScheduleExpiryId}/update")
  ModelAndView submitUpdateLicenceExpiryPage(
      @PathVariable UUID licenceScheduleExpiryId,
      @ModelAttribute("form") LicenceScheduleExpiryForm form,
      BindingResult bindingResult
  ) {
    var expiry = licenceScheduleExpiryService.getExpiryByIdOrThrow(licenceScheduleExpiryId);
    var licenceScheduleDetail = expiry.getLicenceScheduleDetail();

    if (!licenceScheduleExpiryFormValidator.isValidUpdate(form, bindingResult, licenceScheduleDetail)) {
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
