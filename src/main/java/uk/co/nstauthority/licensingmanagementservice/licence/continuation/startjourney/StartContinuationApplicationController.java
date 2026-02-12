package uk.co.nstauthority.licensingmanagementservice.licence.continuation.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanStartApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.SelectApplicationTypeController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/continuation-application/start")
@InvokingUserCanStartApplication
public class StartContinuationApplicationController {

  public static final String PAGE_TITLE = "Start a licence continuation application";

  @GetMapping
  public ModelAndView render() {

    return new ModelAndView("lms/licence/continuation/startJourney")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("startUrl", ReverseRouter.route(on(SelectContinuationApplicationLicenceController.class).render()))
        .addObject("backUrl", ReverseRouter.route(on(SelectApplicationTypeController.class).render()));
  }

}
