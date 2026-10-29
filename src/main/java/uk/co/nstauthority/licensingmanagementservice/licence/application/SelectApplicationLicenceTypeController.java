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
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("licences/start-application")
@InvokingUserCanStartApplication
public class SelectApplicationLicenceTypeController {
  public static final String PAGE_TITLE = "What type of licence is this application related to?";

  private final SelectApplicationLicenceTypeFormValidator selectLicenceTypeFormValidator;
  private final SelectApplicationTypeService selectApplicationTypeService;

  public SelectApplicationLicenceTypeController(
      SelectApplicationLicenceTypeFormValidator selectLicenceTypeFormValidator,
      SelectApplicationTypeService selectApplicationTypeService) {
    this.selectLicenceTypeFormValidator = selectLicenceTypeFormValidator;
    this.selectApplicationTypeService = selectApplicationTypeService;
  }

  @GetMapping
  public ModelAndView renderSelectLicenceType() {
    return getSelectLicenceTypeModelAndView(new SelectApplicationLicenceTypeForm());
  }

  @PostMapping
  ModelAndView submitSelectedLicenceType(
      @ModelAttribute("form") SelectApplicationLicenceTypeForm form,
      BindingResult bindingResult
  ) {
    if (!selectLicenceTypeFormValidator.isValid(form, bindingResult, selectApplicationTypeService.getAvailableLicenceTypes())) {
      return getSelectLicenceTypeModelAndView(form);
    }

    return ReverseRouter.redirect(on(SelectApplicationTypeController.class)
        .render(form.getSelectedLicenceType().getUrlSlug()));
  }

  private ModelAndView getSelectLicenceTypeModelAndView(SelectApplicationLicenceTypeForm selectLicenceTypeForm) {
    var licenceTypeOptions = DisplayableEnumOptionUtil.getDisplayableOptions(
        selectApplicationTypeService.getAvailableLicenceTypes()
    );

    return new ModelAndView("lms/licence/application/selectLicenceType")
        .addObject("form", selectLicenceTypeForm)
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("licenceTypeOptions", licenceTypeOptions)
        .addObject("cancelUrl", ReverseRouter.route(on(WorkAreaController.class).getWorkArea(null, null)));
  }
}
