package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.scheduleworkprogrammeapplication.InvokingUserCanStartScheduleApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("licences/schedule-work-programme-application/{licenceTypeSlug}/start")
@InvokingUserCanStartScheduleApplication
public class StartScheduleWorkProgrammeApplicationJourneyController {

  public static final String PAGE_TITLE = "Start a schedule extension or work programme amendment application";

  @GetMapping
  ModelAndView renderStartScheduleWorkProgrammeApplicationJourney(@PathVariable String licenceTypeSlug) {
    LicenceType licenceType = LicenceType.getFromSlugOrThrow(licenceTypeSlug);

    return new ModelAndView("lms/licence/scheduleWorkProgrammeApplication/startJourney")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceType.getDisplayName())
        .addObject("startUrl", ReverseRouter.route(on(SelectScheduleWorkProgrammeApplicationLicenceController.class)
                .renderSelectLicenceForScheduleWorkProgrammeApplication(licenceType.getUrlSlug()))
        );
  }

}