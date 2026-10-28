package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
public class RemoveEquityChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this beneficial interest change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this beneficial interest change?";
  private static final String REINSTATE_PAGE_TITLE = "Are you sure you want to reinstate this beneficial interest change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeEquityChange";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final EquityChangeService equityChangeService;

  public RemoveEquityChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      EquityChangeService equityChangeService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.equityChangeService = equityChangeService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-equity-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType({
      SetEquityOperation.class,
      TransferEquityOperation.class,
      SubareaSetEquityOperation.class
  })
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderRemoveExecutedEquityChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    return removeEquityChangeModelAndView(correction, licencePosition, change.getId().toString());
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-equity-change")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType({
      SetEquityOperation.class,
      TransferEquityOperation.class,
      SubareaSetEquityOperation.class
  })
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removeEquityChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      RedirectAttributes redirectAttributes
  ) {

    equityChangeService.removeExistingEquityChange(licencePosition, correction, change.getId().toString());

    NotificationBanner.newSuccessBannerWithHeader("Beneficial interest change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition));
  }

  @GetMapping("/change/{changeId}/undo-equity-change")
  public ModelAndView renderUndoEquityChange(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    return undoEquityChangeModelAndView(correction, changeId, positionCorrection);
  }

  @PostMapping("/change/{changeId}/undo-equity-change")
  public ModelAndView undoEquityChange(
      LicenceCorrection correction,
      @PathVariable String changeId,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    equityChangeService.undoEquityChange(correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader(
        reinstating ? "Beneficial interest change reinstated" : "Beneficial interest change undone",
        redirectAttributes
    );

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection));
    }
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition()));
  }

  private ModelAndView removeEquityChangeModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      String changeId
  ) {
    var equityChangeContext = equityChangeService.getExecutedEquityChangeContext(changeId);

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButton", Button.warningButton("Remove beneficial interest change"))
        .addObject("subareaLabel", equityChangeContext.subareaLabel())
        .addObject("setEquityRows", equityChangeContext.setEquityRows())
        .addObject("transferEquityRows", equityChangeContext.transferEquityRows())
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, licencePosition)));
  }

  private ModelAndView undoEquityChangeModelAndView(
      LicenceCorrection correction,
      String changeId,
      LicencePositionCorrection positionCorrection
  ) {
    var equityChangeContext = equityChangeService.getEquityChangeContext(correction, changeId);
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);
    var primaryButton = reinstating
        ? Button.primaryButton("Reinstate beneficial interest change")
        : Button.warningButton("Undo beneficial interest change");

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", reinstating ? REINSTATE_PAGE_TITLE : UNDO_PAGE_TITLE)
        .addObject("primaryButton", primaryButton)
        .addObject("subareaLabel", equityChangeContext.subareaLabel())
        .addObject("setEquityRows", equityChangeContext.setEquityRows())
        .addObject("transferEquityRows", equityChangeContext.transferEquityRows())
        .addObject("cancelUrl", positionPageRoute(correction, positionCorrection));
  }

  private String positionPageRoute(LicenceCorrection correction, LicencePositionCorrection positionCorrection) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection));
    }
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition()));
  }
}