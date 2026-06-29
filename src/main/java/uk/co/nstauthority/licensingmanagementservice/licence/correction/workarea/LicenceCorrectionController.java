package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;

@Controller
@RequestMapping("/licence-corrections")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class LicenceCorrectionController {

  private final LicencePositionService licencePositionService;

  public LicenceCorrectionController(LicencePositionService licencePositionService) {
    this.licencePositionService = licencePositionService;
  }

  @GetMapping("/{correctionId}")
  public ModelAndView renderCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licence = correction.getLicence();

    return new ModelAndView("lms/licence/correction/viewCorrection")
        .addObject("pageTitle", licence.getLicenceReference())
        .addObject("correctionReference", correction.getCorrectionReference())
        .addObject("reason", correction.getReason())
        .addObject("licencePositionTimelineView", licencePositionService.getTimelineView(licence));
  }

}