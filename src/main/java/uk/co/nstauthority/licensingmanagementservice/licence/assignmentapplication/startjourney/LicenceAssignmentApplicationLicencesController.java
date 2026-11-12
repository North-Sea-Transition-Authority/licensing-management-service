package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.licence.internalapi.LicenceInternalApiRestController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/start-application/{licenceTypeSlug}/licence-assignment-application/licences")
@InvokingUserCanStartApplication
public class LicenceAssignmentApplicationLicencesController {

  private final SelectApplicationTypeService selectApplicationTypeService;
  private final LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService;
  private final LicenceAssignmentApplicationLicencesFormValidator formValidator;

  LicenceAssignmentApplicationLicencesController(
      SelectApplicationTypeService selectApplicationTypeService,
      LicenceAssignmentApplicationStartJourneyService licenceAssignmentApplicationStartJourneyService,
      LicenceAssignmentApplicationLicencesFormValidator formValidator
  ) {
    this.selectApplicationTypeService = selectApplicationTypeService;
    this.licenceAssignmentApplicationStartJourneyService = licenceAssignmentApplicationStartJourneyService;
    this.formValidator = formValidator;
  }

  @GetMapping
  public ModelAndView render(LicenceType licenceType) {
    verifyLicenceAssignmentCanBeStarted(licenceType);

    return getModelAndView(licenceType, new LicenceAssignmentApplicationLicencesForm(), List.of());
  }

  @PostMapping
  ModelAndView submit(
      LicenceType licenceType,
      @ModelAttribute("form") LicenceAssignmentApplicationLicencesForm form,
      BindingResult bindingResult
  ) {
    verifyLicenceAssignmentCanBeStarted(licenceType);
    if (!formValidator.isValid(form, bindingResult, licenceType)) {
      var selectedLicences = licenceAssignmentApplicationStartJourneyService.getLicencesOfType(
          form.getLicenceIds(),
          licenceType
      );
      return getModelAndView(licenceType, form, selectedLicences);
    }

    return ReverseRouter.redirect(on(LicenceAssignmentApplicationLicencesController.class).render(licenceType));
  }

  private void verifyLicenceAssignmentCanBeStarted(LicenceType licenceType) {
    selectApplicationTypeService.verifyApplicationTypeCanBeStarted(
        licenceType,
        ApplicationType.LICENCE_ASSIGNMENT_APPLICATION
    );
  }

  private ModelAndView getModelAndView(LicenceType licenceType,
                                       LicenceAssignmentApplicationLicencesForm form,
                                       List<Licence> selectedLicences) {
    return new ModelAndView("lms/licence/assignmentapplication/selectLicences")
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("form", form)
        .addObject("addedLicences", selectedLicences.stream().map(AddedLicence::from).toList())
        .addObject(
            "licenceSearchUrl",
            SearchSelectorService.route(on(LicenceInternalApiRestController.class)
                .searchLicencesByReferenceAndType(licenceType.getUrlSlug(), null))
        )
        .addObject(
            "backUrl",
            ReverseRouter.route(on(LicenceAssignmentApplicationDetailsController.class)
                .render(licenceType, null))
        );
  }

}
