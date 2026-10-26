package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

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
import uk.co.nstauthority.licensingmanagementservice.fds.button.Button;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
public class RemoveSubareaChangeController {

  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this subarea change?";
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

  @GetMapping("/change/{changeId}/undo-subarea-change")
  public ModelAndView renderUndoSubareaChange(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);
    var subareaOperation = subareaChangeService.getStagedSubareaOperationOrThrow(positionCorrection, changeId);

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", UNDO_PAGE_TITLE)
        .addObject("primaryButton", Button.warningButton("Undo subarea change"))
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

    subareaChangeService.undoSubareaChange(positionCorrection, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Subarea change undone", redirectAttributes);

    return ReverseRouter.redirectToUrl(LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, positionCorrection));
  }
}
