package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/start-application/{licenceTypeSlug}/continuation-application/start")
@InvokingUserCanStartApplication
public class StartContinuationApplicationController {

  public static final String PAGE_TITLE = "Start a licence continuation application";

  private final SelectApplicationTypeService selectApplicationTypeService;

  public StartContinuationApplicationController(SelectApplicationTypeService selectApplicationTypeService) {
    this.selectApplicationTypeService = selectApplicationTypeService;
  }

  @GetMapping
  public ModelAndView render(@PathVariable String licenceTypeSlug) {
    var licenceType = selectApplicationTypeService.getLicenceTypeOrThrow(
        licenceTypeSlug,
        ApplicationType.CONTINUATION_APPLICATION
    );

    return new ModelAndView("lms/licence/continuation/startJourney")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("startUrl", ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class)
            .render(licenceType.getUrlSlug())))
        .addObject("backUrl", ReverseRouter.route(on(SelectApplicationTypeController.class)
            .render(licenceType.getUrlSlug())));
  }

}
