package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/schedules")
public class StartLicenceScheduleJourneyController {

  private final SelectCreateUpdateScheduleFormValidator selectCreateUpdateScheduleFormValidator;

  public StartLicenceScheduleJourneyController(SelectCreateUpdateScheduleFormValidator selectCreateUpdateScheduleFormValidator) {
    this.selectCreateUpdateScheduleFormValidator = selectCreateUpdateScheduleFormValidator;
  }

  @GetMapping("/create-or-update")
  ModelAndView renderSelectCreateOrUpdateSchedule() {
    return getSelectCreateOrUpdateScheduleModelAndView(new SelectCreateUpdateScheduleForm());
  }

  @PostMapping("/create-or-update")
  ModelAndView submitSelectCreateOrUpdateSchedule(
      @ModelAttribute("form") SelectCreateUpdateScheduleForm form,
      BindingResult bindingResult
  ) {
    if (!selectCreateUpdateScheduleFormValidator.isValid(form, bindingResult)) {
      return getSelectCreateOrUpdateScheduleModelAndView(form);
    }

    //TODO: add control flow to split between create/update when ability to update is added
    return ReverseRouter.redirect(on(StartLicenceScheduleJourneyController.class).renderStartLicenceScheduleJourney());
  }

  private ModelAndView getSelectCreateOrUpdateScheduleModelAndView(
      SelectCreateUpdateScheduleForm selectCreateUpdateScheduleForm
  ) {
    return new ModelAndView("lms/licence/schedule/selectCreateUpdateSchedule")
        .addObject("form", selectCreateUpdateScheduleForm)
        .addObject("radioOptions", ScheduleJourneyOption.getScheduleJourneyRadioOptions());
  }

  @GetMapping("/start")
  ModelAndView renderStartLicenceScheduleJourney() {
    return new ModelAndView("lms/licence/schedule/startScheduleJourney")
        .addObject("pageTitle", "Create a new licence schedule")
        .addObject("startUrl",
            ReverseRouter.route(on(LicenceScheduleSelectionController.class).renderSelectLicenceForSchedule())
        );
  }

}
