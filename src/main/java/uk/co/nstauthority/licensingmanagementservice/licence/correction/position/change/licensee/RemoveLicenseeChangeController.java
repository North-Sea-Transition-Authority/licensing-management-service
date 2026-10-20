package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.licensee;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicenseeChangeView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
public class RemoveLicenseeChangeController {

  private static final String UNDO_PAGE_TITLE = "Are you sure you want to undo this licensee change?";
  private static final String CONFIRMATION_TEMPLATE = "lms/licence/correction/change/removeLicenseeChange";


  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicenseeChangeService licenseeChangeService;

  public RemoveLicenseeChangeController(LicencePositionCorrectionService licencePositionCorrectionService,
                                        LicenseeChangeService licenseeChangeService) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licenseeChangeService = licenseeChangeService;
  }


  @GetMapping("/change/{changeId}/undo-licensee-change")
  public ModelAndView renderUndoLicenseeChange(
      LicenceCorrection correction,
      @PathVariable String changeId
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    return undoLicenseeChangeModelAndView(correction, positionCorrection);
  }

  @PostMapping("/change/{changeId}/undo-licensee-change")
  public ModelAndView undoLicenseeChange(
      LicenceCorrection correction,
      @PathVariable String changeId,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionContainingChange(correction, changeId);

    licenseeChangeService.undoLicenseeChange(positionCorrection, changeId);

    NotificationBanner.newSuccessBannerWithHeader("Licensee change undone", redirectAttributes);

    if (positionCorrection.getChangeType() == LicencePositionCorrectionChangeType.ADD_POSITION) {
      return ReverseRouter.redirect(on(LicenceCorrectionController.class)
          .renderAddedPosition(correction, positionCorrection));
    }
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition()));
  }

  private ModelAndView undoLicenseeChangeModelAndView(
      LicenceCorrection correction,
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

    return new ModelAndView(CONFIRMATION_TEMPLATE)
        .addObject("pageTitle", UNDO_PAGE_TITLE)
        .addObject("primaryButtonText", "Undo licensee change")
        .addObject("correctedLicenseeChangeView", licenseeChangeView(
            undoLicenseeChangeContext.currentWithdrawingLicenseeNames(),
            undoLicenseeChangeContext.currentJoiningLicenseeNames()
        ))
        .addObject("restoredLicenseeChangeView", restoredLicenseeChangeView)
        .addObject("resultingLicensees", undoLicenseeChangeContext.resultingLicenseeNames())
        .addObject("cancelUrl", positionPageRoute(correction, positionCorrection));
  }

  private static LicenseeChangeView licenseeChangeView(List<String> withdrawingNames, List<String> joiningNames) {
    return new LicenseeChangeView(withdrawingNames, joiningNames, "", ChangeViewUrls.none());
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
