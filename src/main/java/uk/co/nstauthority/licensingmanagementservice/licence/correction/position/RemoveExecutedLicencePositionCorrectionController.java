package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanRemoveLicencePosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionId}/remove")
@InvokingUserCanViewCorrection
@InvokingUserCanRemoveLicencePosition
@LicencePositionBelongsToCorrectionLicence
public class RemoveExecutedLicencePositionCorrectionController {

  private static final String PAGE_TITLE = "Are you sure you want to remove this position?";

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public RemoveExecutedLicencePositionCorrectionController(LicencePositionCorrectionService licencePositionCorrectionService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView renderRemovePosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return removePositionModelAndView(correction, licencePosition);
  }

  @PostMapping
  ModelAndView removePosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      RedirectAttributes redirectAttributes
  ) {
    licencePositionCorrectionService.removeExecutedPosition(correction, licencePosition);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position removed")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class).renderCorrection(correction));
  }

  private ModelAndView removePositionModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return new ModelAndView("lms/licence/correction/removePosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("positionDate", licencePosition.getFormattedPositionDate())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class).renderCorrection(correction)));
  }
}