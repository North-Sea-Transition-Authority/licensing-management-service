package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/positions/{licencePositionCorrectionId}/undo")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
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
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, licenceCorrection);

    return undoPositionModelAndView(licenceCorrection, positionCorrection);
  }

  @PostMapping
  ModelAndView undoPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, licenceCorrection);

    licencePositionCorrectionService.undoPositionCorrection(positionCorrection);

    NotificationBanner.newSuccessBanner()
        .withHeadingContent("Licence correction position undone")
        .applyTo(redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderCorrection(licenceCorrection.getId(), null));
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
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderCorrection(correction.getId(), null)));
  }
}