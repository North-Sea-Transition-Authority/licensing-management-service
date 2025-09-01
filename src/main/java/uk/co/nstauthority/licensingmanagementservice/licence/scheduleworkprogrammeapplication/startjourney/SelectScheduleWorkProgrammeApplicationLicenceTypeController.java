package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("licences/schedule-work-programme-application/licence-type")
public class SelectScheduleWorkProgrammeApplicationLicenceTypeController {
  public static final String PAGE_TITLE = "What type of licence is this application related to?";

  private final SelectScheduleWorkProgrammeApplicationLicenceTypeFormValidator selectLicenceTypeFormValidator;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;

  public SelectScheduleWorkProgrammeApplicationLicenceTypeController(
      SelectScheduleWorkProgrammeApplicationLicenceTypeFormValidator selectLicenceTypeFormValidator,
      LicenceTypeRulesResolver licenceTypeRulesResolver) {
    this.selectLicenceTypeFormValidator = selectLicenceTypeFormValidator;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
  }

  @GetMapping
  ModelAndView renderSelectLicenceType() {
    return getSelectLicenceTypeModelAndView(new SelectScheduleWorkProgrammeApplicationLicenceTypeForm());
  }

  @PostMapping
  ModelAndView submitSelectedLicenceType(
      @ModelAttribute("form") SelectScheduleWorkProgrammeApplicationLicenceTypeForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceTypeFormValidator.isValid(bindingResult)) {
      return getSelectLicenceTypeModelAndView(form);
    }

    return ReverseRouter.redirect(on(StartScheduleWorkProgrammeApplicationJourneyController.class)
        .renderStartScheduleWorkProgrammeApplicationJourney(form.getSelectedLicenceType().getUrlSlug()));
  }

  private ModelAndView getSelectLicenceTypeModelAndView(
      SelectScheduleWorkProgrammeApplicationLicenceTypeForm selectLicenceTypeForm
  ) {
    var licenceTypeOptions = DisplayableEnumOptionUtil.getDisplayableOptions(
        licenceTypeRulesResolver.getLicenceTypesThatCanCreateScheduleWorkProgrammeApplications()
    );

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/selectLicenceType")
        .addObject("form", selectLicenceTypeForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("licenceTypeOptions", licenceTypeOptions);
  }
}
