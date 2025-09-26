package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencestartdate.LicenceStartDateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/schedules/select")
public class LicenceScheduleSelectionController {

  static final String PAGE_TITLE = "What licence do you want to create a schedule for?";

  private final SelectLicenceFormValidator selectLicenceFormValidator;

  public LicenceScheduleSelectionController(
      SelectLicenceFormValidator selectLicenceFormValidator
  ) {
    this.selectLicenceFormValidator = selectLicenceFormValidator;
  }

  @GetMapping
  public ModelAndView renderSelectLicenceForSchedule() {
    return getSelectLicenceModelAndView(new SelectLicenceForm());
  }

  @PostMapping
  ModelAndView submitSelectLicenceForSchedule(
      @ModelAttribute("form") SelectLicenceForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceFormValidator.isValid(form, bindingResult)) {
      return getSelectLicenceModelAndView(form);
    }

    return ReverseRouter.redirect(on(LicenceStartDateController.class)
        .renderLicenceStartDateForm(Integer.parseInt(form.getLicenceId()), null));
  }

  private ModelAndView getSelectLicenceModelAndView(SelectLicenceForm selectLicenceForm) {
    return new ModelAndView("lms/licence/schedule/selectLicence")
        .addObject("form", selectLicenceForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("searchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class).searchLicencesByReference(null))
        )
        .addObject("backUrl",
            ReverseRouter.route(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney())
        );
  }
}
