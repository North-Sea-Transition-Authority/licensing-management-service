package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionCorrectionId}/undo")
@InvokingUserCanViewCorrection
@LicencePositionCorrectionBelongsToCorrection
public class UndoLicencePositionCorrectionController {

  private static final String PAGE_TITLE = "Are you sure you want to undo this position?";

  private final LicencePositionCorrectionService licencePositionCorrectionService;

  public UndoLicencePositionCorrectionController(
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping
  public ModelAndView renderUndoPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return undoPositionModelAndView(licenceCorrection, licencePositionCorrection);
  }

  @PostMapping
  ModelAndView undoPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection licencePositionCorrection,
      RedirectAttributes redirectAttributes
  ) {
    licencePositionCorrectionService.undoPositionCorrection(licencePositionCorrection);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position undone")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class).renderCorrection(licenceCorrection));
  }

  private ModelAndView undoPositionModelAndView(
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection
  ) {
    var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
    var positionDate = payload.effectiveDate();
    return new ModelAndView("lms/licence/correction/undoPosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("positionDate", DateUtil.formatLongDate(positionDate))
        .addObject("correctionReference", payload.correctionReference())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class).renderCorrection(correction)));
  }
}