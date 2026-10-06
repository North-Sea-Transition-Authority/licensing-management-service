package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCanBeReinstantiated;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionId}/reinstate")
@InvokingUserCanViewCorrection
@LicencePositionCanBeReinstantiated
@LicencePositionBelongsToCorrectionLicence
public class ReinstateLicencePositionCorrectionController {

  private static final String PAGE_TITLE = "Are you sure you want to reinstate this position?";

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public ReinstateLicencePositionCorrectionController(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView renderReinstatePosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return reinstatePositionModelAndView(correction, licencePosition);
  }

  @PostMapping
  ModelAndView reinstatePosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      RedirectAttributes redirectAttributes
  ) {
    licencePositionCorrectionService.reinstateDeletedPositionCorrection(correction, licencePosition);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position reinstated")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(correction));
  }

  private ModelAndView reinstatePositionModelAndView(LicenceCorrection correction, LicencePosition licencePosition) {
    return new ModelAndView("lms/licence/correction/reinstatePosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("positionDate", licencePosition.getFormattedPositionDate())
        .addObject("correctionReference", correction.getCorrectionReference())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction)));
  }
}