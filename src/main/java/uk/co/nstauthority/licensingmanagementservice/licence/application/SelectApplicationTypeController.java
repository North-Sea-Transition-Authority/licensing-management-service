package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("licences/start-application")
@InvokingUserCanStartApplication
public class SelectApplicationTypeController {
  public static final String PAGE_TITLE = "What are you applying to do?";

  private final SelectApplicationTypeFormValidator selectApplicationTypeFormValidator;
  private final SelectApplicationTypeService selectApplicationTypeService;

  public SelectApplicationTypeController(
      SelectApplicationTypeFormValidator selectApplicationTypeFormValidator,
      SelectApplicationTypeService selectApplicationTypeService) {
    this.selectApplicationTypeFormValidator = selectApplicationTypeFormValidator;
    this.selectApplicationTypeService = selectApplicationTypeService;
  }

  @GetMapping
  public ModelAndView render() {
    return getModelAndView(new SelectApplicationTypeForm());
  }

  @PostMapping
  ModelAndView submit(
      @ModelAttribute("form") SelectApplicationTypeForm form,
      BindingResult bindingResult
  ) {
    if (!selectApplicationTypeFormValidator.isValid(bindingResult)) {
      return getModelAndView(form);
    }

    return selectApplicationTypeService.getJourneyStartRedirectRoute(form.getSelectedApplicationType());
  }

  private ModelAndView getModelAndView(
      SelectApplicationTypeForm form
  ) {
    var applicationTypeOptions = ApplicationType.getSelectionDisplayOptions();

    return new ModelAndView("lms/licence/application/selectApplicationType")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("applicationTypeOptions", applicationTypeOptions)
        .addObject("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }
}