package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.LogWorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.AddLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply.ReviewCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.update.UpdateCorrectionGeneralDetailsController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionPageView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Controller
@RequestMapping("/licence-corrections")
@InvokingUserCanViewCorrection
@LogWorkAreaItemView(
    itemType = WorkAreaDataItemType.LICENCE_CORRECTION,
    pathVariable = "correctionId"
)
public class LicenceCorrectionController {

  private final LicencePositionService licencePositionService;
  private final LicencePositionViewService licencePositionViewService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicenceService licenceService;
  private final CorrectionDetailsViewService correctionDetailsViewService;

  public LicenceCorrectionController(
      LicencePositionService licencePositionService,
      LicencePositionViewService licencePositionViewService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicenceService licenceService,
      CorrectionDetailsViewService correctionDetailsViewService
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionViewService = licencePositionViewService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licenceService = licenceService;
    this.correctionDetailsViewService = correctionDetailsViewService;
  }

  @GetMapping("/{correctionId}")
  public ModelAndView renderCorrection(LicenceCorrection licenceCorrection) {
    if (isCorrectionApplied(licenceCorrection)) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var licence = licenceCorrection.getLicence();
    var executedLicencePositions = licencePositionService.getExecutedChronologicalLicencePositions(licence);
    var addedPositions = licencePositionCorrectionService.getAddedLicencePositionCorrections(licenceCorrection);

    if (executedLicencePositions.isEmpty() && addedPositions.isEmpty()) {
      return licencePositionsModelAndView(licenceCorrection, LicencePositionPageView.empty());
    }

    if (!executedLicencePositions.isEmpty()) {
      return ReverseRouter.redirect(on(this.getClass()).renderLicencePosition(
          licenceCorrection, executedLicencePositions.getLast()));
    }

    return ReverseRouter.redirect(on(this.getClass()).renderAddedPosition(licenceCorrection, addedPositions.getLast()));
  }

  @GetMapping("/{correctionId}/{licencePositionId}")
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderLicencePosition(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition
  ) {
    if (isCorrectionApplied(licenceCorrection)) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var licencePositionPageView = licencePositionViewService.getCorrectionPositionPageView(licenceCorrection, licencePosition);

    return licencePositionsModelAndView(licenceCorrection, licencePositionPageView);
  }

  @GetMapping("/{correctionId}/added-positions/{licencePositionCorrectionId}")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderAddedPosition(
      LicenceCorrection licenceCorrection,
      LicencePositionCorrection licencePositionCorrection
  ) {
    if (isCorrectionApplied(licenceCorrection)) {
      return appliedCorrectionRedirect(licenceCorrection);
    }

    var licencePositionPageView =
        licencePositionViewService.getCorrectionAddedPositionPageView(licenceCorrection, licencePositionCorrection);

    return licencePositionsModelAndView(licenceCorrection, licencePositionPageView);
  }

  private static boolean isCorrectionApplied(LicenceCorrection licenceCorrection) {
    return LicenceCorrectionStatus.COMPLETE.equals(licenceCorrection.getStatus());
  }

  private static ModelAndView appliedCorrectionRedirect(LicenceCorrection correction) {
    return ReverseRouter.redirect(on(ReviewCorrectionController.class)
        .renderReviewCorrection(correction));
  }

  private ModelAndView licencePositionsModelAndView(
      LicenceCorrection licenceCorrection,
      LicencePositionPageView licencePositionPageView
  ) {
    var licence =  licenceCorrection.getLicence();

    return new ModelAndView("lms/licence/correction/viewCorrection")
        .addObject("pageTitle", "%s - licence correction".formatted(licence.getLicenceReference()))
        .addObject("pageCaption", licenceService.getLicencePageCaption(licence))
        .addObject("licencePositionPageView", licencePositionPageView)
        .addObject("correctionDetails", correctionDetailsViewService.getDetailsView(licenceCorrection))
        .addObject("addPositionUrl",
            ReverseRouter.route(on(AddLicencePositionCorrectionController.class)
                .renderAddLicencePositionCorrection(licenceCorrection)))
        .addObject("updateGeneralDetailsUrl",
            ReverseRouter.route(on(UpdateCorrectionGeneralDetailsController.class)
                .renderUpdateGeneralDetails(licenceCorrection)))
        .addObject("cancelCorrectionUrl", ReverseRouter.route(on(LicenceCorrectionCancelController.class)
            .renderCancelCorrection(licenceCorrection)))
        .addObject("reviewCorrectionUrl", ReverseRouter.route(on(ReviewCorrectionController.class)
            .renderReviewCorrection(licenceCorrection)));
  }

}