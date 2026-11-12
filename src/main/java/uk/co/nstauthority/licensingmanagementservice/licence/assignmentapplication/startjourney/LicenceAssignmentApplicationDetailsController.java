package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryRoleService;

@Controller
@RequestMapping("licences/start-application/{licenceTypeSlug}/licence-assignment-application/details")
@InvokingUserCanStartApplication
public class LicenceAssignmentApplicationDetailsController {

  private final SelectApplicationTypeService selectApplicationTypeService;
  private final IndustryRoleService industryRoleService;
  private final LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService;
  private final LicenceAssignmentApplicationDetailsFormValidator formValidator;

  LicenceAssignmentApplicationDetailsController(
      SelectApplicationTypeService selectApplicationTypeService,
      IndustryRoleService industryRoleService,
      LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService,
      LicenceAssignmentApplicationDetailsFormValidator formValidator
  ) {
    this.selectApplicationTypeService = selectApplicationTypeService;
    this.industryRoleService = industryRoleService;
    this.licenceAssignmentApplicationStartJourneyService = licenceAssignmentApplicationStartJourneyService;
    this.formValidator = formValidator;
  }

  @GetMapping
  public ModelAndView render(
      LicenceType licenceType,
      ServiceUserDetail user
  ) {
    selectApplicationTypeService.verifyApplicationTypeCanBeStarted(
        licenceType,
        ApplicationType.LICENCE_ASSIGNMENT_APPLICATION
    );

    return getModelAndView(licenceType, new LicenceAssignmentApplicationDetailsForm(), user);
  }

  @PostMapping
  ModelAndView submit(
      LicenceType licenceType,
      ServiceUserDetail user,
      @ModelAttribute("form") LicenceAssignmentApplicationDetailsForm form,
      BindingResult bindingResult
  ) {
    selectApplicationTypeService.verifyApplicationTypeCanBeStarted(
        licenceType,
        ApplicationType.LICENCE_ASSIGNMENT_APPLICATION
    );

    var availableOrganisationUnitIds = industryRoleService.getEditorOrSubmitterOrganisationUnitIds(user.wuaId());
    if (!formValidator.isValid(form, bindingResult, availableOrganisationUnitIds)) {
      return getModelAndView(licenceType, form, user);
    }
    //TODO LMS2-223: persist the application details

    return ReverseRouter.redirect(on(LicenceAssignmentApplicationLicencesController.class).render(licenceType));
  }

  private ModelAndView getModelAndView(
      LicenceType licenceType,
      LicenceAssignmentApplicationDetailsForm form,
      ServiceUserDetail user
  ) {
    return new ModelAndView("lms/licence/assignmentapplication/applicationDetails")
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("form", form)
        .addObject("organisationOptions", licenceAssignmentApplicationStartJourneyService.getOrganisationOptions(user))
        .addObject(
            "backUrl",
            ReverseRouter.route(on(SelectApplicationTypeController.class).render(licenceType.getUrlSlug()))
        );
  }

}
