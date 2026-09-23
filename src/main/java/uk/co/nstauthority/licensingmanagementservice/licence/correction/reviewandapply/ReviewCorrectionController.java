package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.LicencePositionValidationService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.TabbedLicencePageService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}/review")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
public class ReviewCorrectionController {

  private static final String POSITIONS = "positions";
  private static final String CAN_APPLY = "canApply";

  private final EnergyPortalUserService energyPortalUserService;
  private final LicenceService licenceService;
  private final CorrectedTimelineService correctedTimelineService;
  private final CorrectionReviewService correctionReviewService;
  private final LicencePositionValidationService licencePositionValidationService;
  private final CorrectionApplyService correctionApplyService;
  private final TabbedLicencePageService tabbedLicencePageService;

  public ReviewCorrectionController(
      EnergyPortalUserService energyPortalUserService,
      LicenceService licenceService,
      CorrectedTimelineService correctedTimelineService,
      CorrectionReviewService correctionReviewService,
      LicencePositionValidationService licencePositionValidationService,
      CorrectionApplyService correctionApplyService,
      TabbedLicencePageService tabbedLicencePageService
  ) {
    this.energyPortalUserService = energyPortalUserService;
    this.licenceService = licenceService;
    this.correctedTimelineService = correctedTimelineService;
    this.correctionReviewService = correctionReviewService;
    this.licencePositionValidationService = licencePositionValidationService;
    this.correctionApplyService = correctionApplyService;
    this.tabbedLicencePageService = tabbedLicencePageService;
  }

  @GetMapping
  @CorrectionHasStatus({LicenceCorrectionStatus.IN_PROGRESS, LicenceCorrectionStatus.COMPLETE})
  public ModelAndView renderReviewCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection
  ) {
    if (LicenceCorrectionStatus.COMPLETE.equals(licenceCorrection.getStatus())) {
      return licencePositionsModelAndView(correctionId, licenceCorrection, true)
          .addObject(POSITIONS, correctionReviewService.getAppliedPositions(licenceCorrection))
          .addObject(CAN_APPLY, false)
          .addObject("errorSummaryItems", List.of());
    }

    var correctedTimeline = correctedTimelineService.getCorrectedTimeline(licenceCorrection);
    var blockingErrors = licencePositionValidationService.validate(
        correctedTimeline.positionsToApply(),
        correctedTimeline.resolvedStates(),
        correctedTimeline.isCarbonStorage()
    );

    return reviewCorrectionModelAndView(correctionId, licenceCorrection, correctedTimeline, blockingErrors);
  }

  @PostMapping
  public ModelAndView processApplyCorrection(
      @PathVariable UUID correctionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection licenceCorrection,
      RedirectAttributes redirectAttributes
  ) {
    var blockingErrors = correctionApplyService.applyCorrection(licenceCorrection);

    if (!blockingErrors.isEmpty()) {
      return reviewCorrectionModelAndView(
          correctionId,
          licenceCorrection,
          correctedTimelineService.getCorrectedTimeline(licenceCorrection),
          blockingErrors
      );
    }

    NotificationBanner.newSuccessBannerWithHeader(
        "Correction %s applied".formatted(licenceCorrection.getCorrectionReference()),
        redirectAttributes
    );

    return ReverseRouter.redirectToUrl(tabbedLicencePageService.getDefaultTabUrl(licenceCorrection.getLicence()));
  }

  private ModelAndView reviewCorrectionModelAndView(
      UUID correctionId,
      LicenceCorrection licenceCorrection,
      CorrectedTimeline correctedTimeline,
      List<PositionValidationError> blockingErrors
  ) {
    var positions = correctionReviewService.getReviewPositions(correctedTimeline, blockingErrors);

    return licencePositionsModelAndView(correctionId, licenceCorrection, false)
        .addObject(POSITIONS, positions)
        .addObject(CAN_APPLY, blockingErrors.isEmpty() && !positions.isEmpty())
        .addObject("errorSummaryItems", PositionValidationError.toErrorSummaryItems(blockingErrors));
  }

  private ModelAndView licencePositionsModelAndView(
      UUID correctionId,
      LicenceCorrection licenceCorrection,
      boolean isCorrectionApplied
  ) {
    var allocatedToUserDetail = energyPortalUserService.getByWuaId(
        WebUserAccountId.from(licenceCorrection.getAllocatedToWuaId()),
        "Get correction allocated to user details"
    );

    var backLinkUrl = isCorrectionApplied
        ? tabbedLicencePageService.getDefaultTabUrl(licenceCorrection.getLicence())
        : ReverseRouter.route(on(LicenceCorrectionController.class).renderCorrection(correctionId, null));

    return new ModelAndView("lms/licence/correction/reviewandapply/reviewCorrection")
        .addObject("pageCaption", licenceService.getLicencePageCaption(licenceCorrection.getLicence()))
        .addObject("pageTitle", isCorrectionApplied
            ? "Correction %s".formatted(licenceCorrection.getCorrectionReference())
            : "Do you want to apply this correction?")
        .addObject("correction", licenceCorrection)
        .addObject("allocatedToUser", allocatedToUserDetail.displayName())
        .addObject("createdDate", DateUtil.formatLongDate(licenceCorrection.getCreatedInstant()))
        .addObject("isCorrectionApplied", isCorrectionApplied)
        .addObject("backLinkUrl", backLinkUrl);
  }
}
