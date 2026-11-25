package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionStagedPayloadBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionStagedPayloadId}/undo")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@LicencePositionStagedPayloadBelongsToCorrection
public class UndoLicencePositionStagedPayloadController {

  private static final String PAGE_TITLE = "Are you sure you want to undo this position?";

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;

  public UndoLicencePositionStagedPayloadController(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
  }

  @GetMapping
  public ModelAndView renderUndoPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload licencePositionStagedPayload
  ) {
    return undoPositionModelAndView(licenceCorrection, licencePositionStagedPayload);
  }

  @PostMapping
  ModelAndView undoPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload licencePositionStagedPayload,
      RedirectAttributes redirectAttributes
  ) {
    licencePositionStagedPayloadService.undoPositionStagedPayload(licencePositionStagedPayload);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position undone")
        .applyTo(redirectAttributes);

    // Undoing an added position deletes it, so there is no position left to return to.
    if (licencePositionStagedPayload.getChangeType() == LicencePositionStagedPayloadChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class).renderCorrection(licenceCorrection, null));
    }

    return LicencePositionStagedPayloadRouteUtil.redirectToPositionPage(
        licenceCorrection,
        licencePositionStagedPayload.getTargetLicencePosition().getId()
    );
  }

  private ModelAndView undoPositionModelAndView(
      LicenceCorrection correction,
      LicencePositionStagedPayload positionStagedPayload
  ) {
    var payload = (CreateLicencePositionPayload) positionStagedPayload.getPayload();
    var positionDate = payload.effectiveDate();
    return new ModelAndView("lms/licence/correction/undoPosition")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("positionDate", DateUtil.formatLongDate(positionDate))
        .addObject("correctionReference", payload.correctionReference())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class).renderCorrection(correction, null)));
  }
}
