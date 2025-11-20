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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;

@Controller
@RequestMapping("licence/schedule/{licenceScheduleDetailId}/rates/create")
public class LicenceScheduleRateController {

  private final LicenceScheduleRateFormService licenceScheduleRateFormService;
  private final LicenceScheduleRateFormValidator licenceScheduleRateFormValidator;
  private final LicenceService licenceService;

  public LicenceScheduleRateController(
      LicenceScheduleRateFormService licenceScheduleRateFormService,
      LicenceScheduleRateFormValidator licenceScheduleRateFormValidator,
      LicenceService licenceService
  ) {
    this.licenceScheduleRateFormService = licenceScheduleRateFormService;
    this.licenceScheduleRateFormValidator = licenceScheduleRateFormValidator;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderNewLicenceScheduleRateForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getScheduleRateModelAndView(new LicenceScheduleRateForm(), licenceScheduleDetail);
  }

  @PostMapping
  ModelAndView submitNewLicenceScheduleRateForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceScheduleRateForm form,
      BindingResult bindingResult
  ) {
    if (!licenceScheduleRateFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getScheduleRateModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleRateFormService.saveRateFromForm(form, licenceScheduleDetail);

    return licenceScheduleDetail.getScheduleTimelineRedirectUrl();
  }

  private ModelAndView getScheduleRateModelAndView(LicenceScheduleRateForm form, LicenceScheduleDetail licenceScheduleDetail) {
    return new ModelAndView("lms/licence/schedule/createScheduleRate")
        .addObject("form", form)
        .addObject("termOptions", licenceScheduleRateFormService.getScheduleTermOptions(licenceScheduleDetail))
        .addObject("phaseOptions", licenceScheduleRateFormService.getSchedulePhaseOptions(licenceScheduleDetail))
        .addObject("rateDefinitionOptions", licenceScheduleRateFormService.getRateDefinitionOptions(licenceScheduleDetail))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licenceScheduleDetail.getLicenceSchedule().getLicence()))
        .addObject("cancelUrl", licenceScheduleDetail.getScheduleTimelineRouteUrl());
  }
}
