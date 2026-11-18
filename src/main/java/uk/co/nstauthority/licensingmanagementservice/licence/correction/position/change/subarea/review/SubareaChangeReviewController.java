package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.SubareaChangeValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class SubareaChangeReviewController {

  private static final String STAGED_REVIEW_PATH =
      "/position-correction/{licencePositionCorrectionId}/subarea-change/{blockFeatureId}/review";
  private static final String LIVE_REVIEW_PATH = "/position/{licencePositionId}/change/{changeId}/subarea-change-review";
  private static final String CHANGE_SUBAREAS_PATH = "/change-subareas";
  private static final String PAGE_TITLE = "Review and submit subarea changes for Block %s";
  private static final String SAVED_BANNER = "Subarea change saved";
  private static final String NO_CHANGE_ERROR = "You must make a change to the subareas before submitting";

  private final SubareaChangeSummarySectionService subareaChangeSummarySectionService;
  private final SubareaChangeService subareaChangeService;
  private final SubareaChangeValidator subareaChangeValidator;
  private final LicencePositionSpatialService licencePositionSpatialService;
  private final FeatureService featureService;

  SubareaChangeReviewController(
      SubareaChangeSummarySectionService subareaChangeSummarySectionService,
      SubareaChangeService subareaChangeService,
      SubareaChangeValidator subareaChangeValidator,
      LicencePositionSpatialService licencePositionSpatialService,
      FeatureService featureService
  ) {
    this.subareaChangeSummarySectionService = subareaChangeSummarySectionService;
    this.subareaChangeService = subareaChangeService;
    this.subareaChangeValidator = subareaChangeValidator;
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.featureService = featureService;
  }

  @GetMapping(STAGED_REVIEW_PATH)
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderStagedReview(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID blockFeatureId,
      ServiceUserDetail user
  ) {
    return getStagedReviewModelAndView(correction, licencePositionCorrection, blockFeatureId, user);
  }

  @PostMapping(STAGED_REVIEW_PATH)
  @CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
  @LicencePositionCorrectionBelongsToCorrection
  ModelAndView submit(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID blockFeatureId,
      ServiceUserDetail user,
      RedirectAttributes redirectAttributes
  ) {
    var commandJourneyId = subareaChangeService
        .getSubareaOperationOrThrow(licencePositionCorrection, blockFeatureId)
        .commandJourneyIdOrThrow();

    if (subareaChangeValidator.hasErrors(commandJourneyId)) {
      return getStagedReviewModelAndView(correction, licencePositionCorrection, blockFeatureId, user)
          .addObject("errorSummaryItems", List.of(new ErrorSummaryItem(1, "summaryaccordion-subarea-changes", NO_CHANGE_ERROR)));
    }

    subareaChangeService.completeSubareaChange(licencePositionCorrection, blockFeatureId);

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirectToUrl(LicencePositionCorrectionRouteUtil.getPositionPageUrl(
        correction,
        licencePositionCorrection
    ));
  }

  @PostMapping(STAGED_REVIEW_PATH + CHANGE_SUBAREAS_PATH)
  @CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
  @LicencePositionCorrectionBelongsToCorrection
  ModelAndView changeStagedSubareas(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID blockFeatureId
  ) {
    subareaChangeService.getOrCreateCommandJourney(licencePositionCorrection, blockFeatureId);

    return ReverseRouter.redirect(on(SubareaChangeController.class)
        .renderSubareaChange(correction, licencePositionCorrection, blockFeatureId));
  }

  @GetMapping(LIVE_REVIEW_PATH)
  @CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(SubareaOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderLiveReview(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      ServiceUserDetail user
  ) {
    var operation = subareaChangeService.getLiveSubareaOperationOrThrow(change);
    var blockFeatureId = operation.blockFeatureId();
    var stateBefore = licencePositionSpatialService.getStateGoingIntoChange(
        correction,
        licencePosition.getId(),
        change.getId().toString()
    );
    var context = new SubareaChangeSummaryContext.LiveChange(
        blockFeatureId,
        stateBefore.subareasOf(blockFeatureId),
        operation.transfers(),
        stateBefore,
        licencePositionSpatialService.getStateGoingIntoChange(correction, licencePosition.getId(), null),
        ReverseRouter.route(on(SubareaChangeReviewController.class).changeLiveSubareas(correction, licencePosition, change))
    );

    return getReviewModelAndView(
        correction,
        blockFeatureId,
        subareaChangeSummarySectionService.getSummarySections(context, user)
    );
  }

  @PostMapping(LIVE_REVIEW_PATH + CHANGE_SUBAREAS_PATH)
  @CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(SubareaOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  ModelAndView changeLiveSubareas(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    var positionCorrection = subareaChangeService.correctExistingSubareaChange(correction, licencePosition, change);
    var blockFeatureId = subareaChangeService.getLiveSubareaOperationOrThrow(change).blockFeatureId();

    return ReverseRouter.redirect(on(SubareaChangeController.class)
        .renderSubareaChange(correction, positionCorrection, blockFeatureId));
  }

  private ModelAndView getStagedReviewModelAndView(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      UUID blockFeatureId,
      ServiceUserDetail user
  ) {
    var changeId = subareaChangeService.getSubareaChangeOrThrow(licencePositionCorrection, blockFeatureId).changeId();
    var operation = subareaChangeService.getSubareaOperationOrThrow(licencePositionCorrection, blockFeatureId);
    var commandJourneyId = operation.commandJourneyId();
    var originalSubareas = licencePositionSpatialService.getStateGoingIntoChange(licencePositionCorrection, changeId)
        .subareasOf(blockFeatureId);
    var context = new SubareaChangeSummaryContext.Staged(
        blockFeatureId,
        originalSubareas,
        commandJourneyId == null
            ? operation.transfers()
            : subareaChangeService.getTransfers(commandJourneyId, originalSubareas),
        licencePositionSpatialService.getStateAtEndOfPositionWithoutChange(licencePositionCorrection, changeId),
        ReverseRouter.route(on(SubareaChangeReviewController.class)
            .changeStagedSubareas(correction, licencePositionCorrection, blockFeatureId))
    );

    var modelAndView = getReviewModelAndView(
        correction,
        blockFeatureId,
        subareaChangeSummarySectionService.getSummarySections(context, user)
    );

    if (commandJourneyId != null) {
      modelAndView
          .addObject(
              "submitUrl",
              ReverseRouter.route(on(SubareaChangeReviewController.class)
                  .submit(correction, licencePositionCorrection, blockFeatureId, null, null))
          )
          .addObject(
              "backLinkUrl",
              ReverseRouter.route(on(SubareaChangeController.class)
                  .renderSubareaChange(correction, licencePositionCorrection, blockFeatureId))
          );
    }
    return modelAndView;
  }

  private ModelAndView getReviewModelAndView(
      LicenceCorrection correction,
      UUID blockFeatureId,
      List<SummarySection> summarySections
  ) {
    var blockName = featureService.getFeatureOrThrow(blockFeatureId).getFeatureName();

    return new ModelAndView("lms/licence/correction/change/subarea/subareaChangeReview")
        .addObject("pageTitle", PAGE_TITLE.formatted(blockName))
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("summarySections", summarySections);
  }
}
