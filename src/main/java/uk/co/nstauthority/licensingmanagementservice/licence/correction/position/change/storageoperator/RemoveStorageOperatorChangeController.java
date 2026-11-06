package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.storageoperator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
public class RemoveStorageOperatorChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this storage operator change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this storage operator change?";
  private static final String REINSTATE_PAGE_TITLE = "Are you sure you want to reinstate this storage operator change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeStorageOperatorChange";

  private final LicencePositionViewService licencePositionViewService;
  private final StorageOperatorChangeService storageOperatorChangeService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;

  RemoveStorageOperatorChangeController(
      LicencePositionViewService licencePositionViewService,
      StorageOperatorChangeService storageOperatorChangeService,
      LicencePositionCorrectionService licencePositionCorrectionService
  ) {
    this.licencePositionViewService = licencePositionViewService;
    this.storageOperatorChangeService = storageOperatorChangeService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-storage-operator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(StorageOperatorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderRemoveExecutedStorageOperatorChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange
  ) {
    return removeStorageOperatorChangeModelAndView(correction, licencePosition);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-storage-operator-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(StorageOperatorOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  ModelAndView removeStorageOperatorChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      RedirectAttributes redirectAttributes
  ) {
    storageOperatorChangeService
        .removeExistingStorageOperatorChange(licencePosition, correction, change.getId().toString());

    NotificationBanner.newSuccessBannerWithHeader("Storage operator change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  @GetMapping("/change/{changeId}/undo-storage-operator-change")
  public ModelAndView renderUndoStorageOperatorChange(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    return undoStorageOperatorChangeModelAndView(correction, changeId, positionCorrection);
  }

  @PostMapping("/change/{changeId}/undo-storage-operator-change")
  ModelAndView undoStorageOperatorChange(
      LicenceCorrection correction,
      @PathVariable String changeId,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    storageOperatorChangeService.undoStorageOperatorChange(correction, changeId);

    NotificationBanner.newSuccessBannerWithHeader(
        reinstating ? "Storage operator change reinstated" : "Storage operator change undone",
        redirectAttributes
    );

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection, null));
    }
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition(), null));
  }

  private ModelAndView removeStorageOperatorChangeModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var storageOperatorChangeContext = licencePositionViewService
        .getStorageOperatorChangeContext(correction, licencePosition.getId());

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButton", Button.warningButton("Remove storage operator change"))
        .addObject("withdrawingStorageOperatorName", storageOperatorChangeContext.previousOperatorName())
        .addObject("joiningStorageOperatorName", storageOperatorChangeContext.currentOperatorName())
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
                .renderLicencePosition(correction, licencePosition, null)));
  }

  private ModelAndView undoStorageOperatorChangeModelAndView(
      LicenceCorrection correction,
      String changeId,
      LicencePositionCorrection positionCorrection
  ) {
    var storageOperatorChangeContext =
        licencePositionViewService.getStorageOperatorChangeContext(correction, getPositionId(positionCorrection));
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    var primaryButton = reinstating
        ? Button.primaryButton("Reinstate storage operator change")
        : Button.warningButton("Undo storage operator change");

    var joiningStorageOperatorName = reinstating
        ? storageOperatorChangeService.getJoiningOperatorForReinstating(correction, positionCorrection)
        : storageOperatorChangeContext.currentOperatorName();

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", reinstating ? REINSTATE_PAGE_TITLE : UNDO_PAGE_TITLE)
        .addObject("primaryButton", primaryButton)
        .addObject("withdrawingStorageOperatorName", storageOperatorChangeContext.previousOperatorName())
        .addObject("joiningStorageOperatorName", joiningStorageOperatorName)
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

  private UUID getPositionId(LicencePositionCorrection positionCorrection) {
    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      var payload = (CreateLicencePositionPayload) positionCorrection.getPayload();
      return UUID.fromString(payload.licencePositionId());
    }
    return positionCorrection.getTargetLicencePosition().getId();
  }
}
