package uk.co.nstauthority.licensingmanagementservice.licence.position;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;

@Controller
@RequestMapping("licences/{licenceId}/timeline")
@Profile("enable-lms2")
public class LicencePositionTimelineController {

  //TODO LMS2-52: Access to timeline, licence position and schedule information for a licence
  private static final String PAGE_TITLE = "Licence positions";

  private final LicencePositionService licencePositionService;
  private final LicenceService licenceService;

  public LicencePositionTimelineController(LicencePositionService licencePositionService, LicenceService licenceService) {
    this.licencePositionService = licencePositionService;
    this.licenceService = licenceService;
  }

  @GetMapping
  public ModelAndView renderLicencePositionTimeline(
      Licence licence
  ) {
    return new ModelAndView("lms/licence/position/licencePositionTimeline")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("licencePositionTimelineView", licencePositionService.getTimelineView(licence));
  }
}