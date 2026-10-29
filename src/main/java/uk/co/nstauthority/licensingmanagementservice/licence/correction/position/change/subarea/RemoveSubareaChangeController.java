package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
public class RemoveSubareaChangeController {

  private static final String REMOVE_PAGE_TITLE = "Are you sure you want to remove this subarea change?";
  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this subarea change?";
  private static final String REINSTATE_PAGE_TITLE = "Are you sure you want to reinstate this subarea change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/subarea/removeSubareaChange";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final SubareaChangeService subareaChangeService;

  public RemoveSubareaChangeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      SubareaChangeService subareaChangeService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.subareaChangeService = subareaChangeService;
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/remove-subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(SubareaOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderRemoveExecutedSubareaChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange
  ) {
    var subareaOperation = subareaChangeService.getLiveSubareaOperationOrThrow(licencePositionChange);

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", REMOVE_PAGE_TITLE)
        .addObject("primaryButton", Button.warningButton("Remove subarea change"))
        .addObject("blockName", subareaChangeService.getBlockName(subareaOperation))
        .addObject("cancelUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, licencePosition, null)));
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/remove-subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(SubareaOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView removeSubareaChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange licencePositionChange,
      RedirectAttributes redirectAttributes
  ) {
    licencePositionCorrectionService.stageRemovalOfExecutedChange(
        correction,
        licencePosition,
        licencePositionChange.getId().toString()
    );

    NotificationBanner.newSuccessBannerWithHeader("Subarea change removed", redirectAttributes);

    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, licencePosition, null));
  }

  @GetMapping("/change/{changeId}/undo-subarea-change")
  public ModelAndView renderUndoSubareaChange(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);
    var subareaOperation = subareaChangeService.getStagedSubareaOperationOrThrow(positionCorrection, changeId);
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    var primaryButton = reinstating
        ? Button.primaryButton("Reinstate subarea change")
        : Button.warningButton("Undo subarea change");

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", reinstating ? REINSTATE_PAGE_TITLE : UNDO_PAGE_TITLE)
        .addObject("primaryButton", primaryButton)
        .addObject("blockName", subareaChangeService.getBlockName(subareaOperation))
        .addObject("cancelUrl", LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, positionCorrection));
  }

  @PostMapping("/change/{changeId}/undo-subarea-change")
  public ModelAndView undoSubareaChange(
      LicenceCorrection correction,
      @PathVariable String changeId,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);
    var reinstating = licencePositionCorrectionService.isStagedRemoval(positionCorrection, changeId);

    subareaChangeService.undoSubareaChange(positionCorrection, changeId);

    NotificationBanner.newSuccessBannerWithHeader(
        reinstating ? "Subarea change reinstated" : "Subarea change undone",
        redirectAttributes
    );

    return ReverseRouter.redirectToUrl(LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, positionCorrection));
  }
}
