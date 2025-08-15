package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm;

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
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/licence/schedule/{licenceScheduleDetailId}/term")
public class LicenceScheduleTermController {

  private LicenceScheduleTermService licenceScheduleTermService;
  private LicenceScheduleTermFormValidator licenceScheduleTermFormValidator;

  public LicenceScheduleTermController(
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceScheduleTermFormValidator licenceScheduleTermFormValidator
  ) {
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceScheduleTermFormValidator = licenceScheduleTermFormValidator;
  }

  @GetMapping("/create")
  public ModelAndView renderAddNewTermForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail
  ) {
    return getScheduleTermModelAndView(new LicenceScheduleTermForm(), licenceScheduleDetail);
  }

  @PostMapping("/create")
  ModelAndView submitAddNewTermForm(
      @PathVariable UUID licenceScheduleDetailId,
      LicenceScheduleDetail licenceScheduleDetail,
      @ModelAttribute("form") LicenceScheduleTermForm form,
      BindingResult bindingResult
  ) {
    if (!licenceScheduleTermFormValidator.isValid(form, bindingResult, licenceScheduleDetail)) {
      return getScheduleTermModelAndView(form, licenceScheduleDetail);
    }

    licenceScheduleTermService.saveTermFromForm(form, licenceScheduleDetail);

    // TODO: LMS1-135 redirect to licence schedule timeline
    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  private ModelAndView getScheduleTermModelAndView(LicenceScheduleTermForm form, LicenceScheduleDetail licenceScheduleDetail) {
    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    return new ModelAndView("lms/licence/schedule/createScheduleTerm")
        .addObject("form", form)
        .addObject("radioOptions", TermType.getTermRadioOptionsFor(licenceType));
  }
}
