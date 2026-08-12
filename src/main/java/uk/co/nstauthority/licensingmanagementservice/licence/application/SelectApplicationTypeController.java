package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("licences/start-application")
@InvokingUserCanStartApplication
public class SelectApplicationTypeController {
  public static final String PAGE_TITLE = "What are you applying to do?";

  private final SelectApplicationTypeFormValidator selectApplicationTypeFormValidator;
  private final SelectApplicationTypeService selectApplicationTypeService;
  private final FeatureFlagService featureFlagService;

  public SelectApplicationTypeController(
      SelectApplicationTypeFormValidator selectApplicationTypeFormValidator,
      SelectApplicationTypeService selectApplicationTypeService,
      FeatureFlagService featureFlagService) {
    this.selectApplicationTypeFormValidator = selectApplicationTypeFormValidator;
    this.selectApplicationTypeService = selectApplicationTypeService;
    this.featureFlagService = featureFlagService;
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
    if (!selectApplicationTypeFormValidator.isValid(form, bindingResult)) {
      return getModelAndView(form);
    }

    return selectApplicationTypeService.getJourneyStartRedirectRoute(form.getSelectedApplicationType());
  }

  private ModelAndView getModelAndView(
      SelectApplicationTypeForm form
  ) {
    var applicationTypeOptions = ApplicationType.getSelectionDisplayOptions(
        featureFlagService.filterEnabled(Arrays.asList(ApplicationType.values())));

    return new ModelAndView("lms/licence/application/selectApplicationType")
        .addObject("form", form)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("applicationTypeOptions", applicationTypeOptions)
        .addObject("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }
}