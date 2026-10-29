package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

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
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
public class RemoveLicenseeChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this licensee change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this licensee change?";
  private static final String REINSTATE_PAGE_TITLE = "Are you sure you want to reinstate this licensee change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeLicenseeChange";


  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionViewService licencePositionViewService;
  private final LicenseeChangeService licenseeChangeService;

  public RemoveLicenseeChangeController(LicencePositionCorrectionService licencePositionCorrectionService,
                                        LicencePositionViewService licencePositionViewService,
                                        LicenseeChangeService licenseeChangeService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionViewService = licencePositionViewService;
    this.licenseeChangeService = licenseeChangeService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-licensee-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(LicenseeOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderRemoveExecutedLicenseeChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange
  ) {
    return removeLicenseeChangeModelAndView(correction, licencePosition);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-licensee-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(LicenseeOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removeLicenseeChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      RedirectAttributes redirectAttributes
  ) {
    licenseeChangeService.removeExistingLicenseeChange(
        licencePosition, correction, change.getId().toString());

    NotificationBanner.newSuccessBannerWithHeader("Licensee change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  @GetMapping("/change/{changeId}/undo-licensee-change")
  public ModelAndView renderUndoLicenseeChange(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    return undoLicenseeChangeModelAndView(correction, changeId, positionCorrection);
  }

  @PostMapping("/change/{changeId}/undo-licensee-change")
  public ModelAndView undoLicenseeChange(
      LicenceCorrection correction,
      @PathVariable String changeId,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    licenseeChangeService.undoLicenseeChange(positionCorrection, changeId);

    NotificationBanner.newSuccessBannerWithHeader(
        reinstating ? "Licensee change reinstated" : "Licensee change undone",
        redirectAttributes);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection, null));
    }
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition(), null));
  }

  private ModelAndView removeLicenseeChangeModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var licenseeChangeContext =
        licencePositionViewService.getLicenseeChangeContext(correction, licencePosition.getId());

    var correctedLicenseeChangeView = new LicenseeChangeView(
        licenseeChangeContext.currentWithdrawingLicenseeNames(),
        licenseeChangeContext.currentJoiningLicenseeNames(),
        "",
        ChangeViewUrls.none()
    );

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButton", Button.warningButton("Remove licensee change"))
        .addObject("correctedLicenseeChangeView", correctedLicenseeChangeView)
        .addObject("resultingLicensees", licenseeChangeContext.previousLicenseeNames())
        .addObject("cancelUrl",
            ReverseRouter.route(on(LicenceCorrectionController.class).renderLicencePosition(correction, licencePosition, null)));
  }

  private ModelAndView undoLicenseeChangeModelAndView(
      LicenceCorrection correction,
      String changeId,
      LicencePositionCorrection positionCorrection
  ) {
    var undoLicenseeChangeContext = licenseeChangeService
        .getUndoLicenseeChangeContext(
            correction,
            positionCorrection,
            positionCorrection.getPositionId()
        );

    LicenseeChangeView restoredLicenseeChangeView = licenseeChangeService
        .getRestoredLicenseeChangeView(
            undoLicenseeChangeContext.restoredWithdrawingLicenseeNames(),
            undoLicenseeChangeContext.restoredJoiningLicenseeNames()
        );

    var correctedLicenseeChangeView = new LicenseeChangeView(
        undoLicenseeChangeContext.currentWithdrawingLicenseeNames(),
        undoLicenseeChangeContext.currentJoiningLicenseeNames(),
        "",
        ChangeViewUrls.none()
    );

    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    var primaryButton = reinstating
        ? Button.primaryButton("Reinstate licensee change")
        : Button.warningButton("Undo licensee change");

    if (reinstating) {
      correctedLicenseeChangeView = null;
    }

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", reinstating ? REINSTATE_PAGE_TITLE : UNDO_PAGE_TITLE)
        .addObject("primaryButton", primaryButton)
        .addObject("correctedLicenseeChangeView", correctedLicenseeChangeView)
        .addObject("restoredLicenseeChangeView", restoredLicenseeChangeView)
        .addObject("resultingLicensees", undoLicenseeChangeContext.resultingLicenseeNames())
        .addObject("cancelUrl", positionPageRoute(correction, positionCorrection));
  }

  private String positionPageRoute(LicenceCorrection correction, LicencePositionCorrection positionCorrection) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection, null));
    }
    return ReverseRouter.route(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition(), null));
  }
}
