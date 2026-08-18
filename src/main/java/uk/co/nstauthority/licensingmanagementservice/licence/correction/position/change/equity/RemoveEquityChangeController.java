package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
public class RemoveEquityChangeController {

  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this beneficial interest change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/undoEquityChange";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final EquityChangeService equityChangeService;

  public RemoveEquityChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      EquityChangeService equityChangeService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.equityChangeService = equityChangeService;
  }

  @GetMapping("/change/{changeId}/undo-equity-change")
  public ModelAndView renderUndoEquityChange(
      @PathVariable UUID correctionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    return undoEquityChangeModelAndView(correction, changeId, positionCorrection);
  }

  @PostMapping("/change/{changeId}/undo-equity-change")
  public ModelAndView undoEquityChange(
      @PathVariable UUID correctionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    equityChangeService.undoEquityChange(correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Beneficial interest change undone", redirectAttributes);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction.getId(), positionCorrection.getId(), null));
    }
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), positionCorrection.getTargetLicencePosition().getId(), null));
  }

  private ModelAndView undoEquityChangeModelAndView(
      LicenceCorrection correction,
      String changeId,
      LicencePositionCorrection positionCorrection
  ) {
    var undoView = equityChangeService.getEquityChangeUndoView(correction, changeId);

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", UNDO_PAGE_TITLE)
        .addObject("primaryButtonText", "Undo beneficial interest change")
        .addObject("setEquityRows", undoView.setEquityRows())
        .addObject("transferEquityRows", undoView.transferEquityRows())
        .addObject("cancelUrl", positionPageRoute(correction, positionCorrection));
  }

  private String positionPageRoute(LicenceCorrection correction, LicencePositionCorrection positionCorrection) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction.getId(), positionCorrection.getId(), null));
    }
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction.getId(), positionCorrection.getTargetLicencePosition().getId(), null));
  }
}