package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionStagedPayloadBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderReviewAndSubmitController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "/licence-corrections/{correctionId}/position-staged-payload" +
        "/{licencePositionStagedPayloadId}/partial-surrender/{featureId}"
)
@InvokingUserCanViewCorrection
@LicencePositionStagedPayloadBelongsToCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderDefineAreaController {

  private static final String DEFINE_AREA_PAGE_TITLE = "Define area to surrender";
  private static final String SELECT_AREAS_PAGE_TITLE = "Select the areas to surrender";
  private static final String NO_SPLIT_ERROR = "You must split the block before continuing";

  private final CommandJourneyService commandJourneyService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;
  private final PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator;
  private final PartialSurrenderValidator partialSurrenderValidator;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final FeatureRelationshipService featureRelationshipService;

  public PartialSurrenderDefineAreaController(
      CommandJourneyService commandJourneyService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator,
      PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator,
      PartialSurrenderValidator partialSurrenderValidator,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      FeatureRelationshipService featureRelationshipService
  ) {
    this.commandJourneyService = commandJourneyService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderSelectAreasFormValidator = partialSurrenderSelectAreasFormValidator;
    this.partialSurrenderDefineAreaValidator = partialSurrenderDefineAreaValidator;
    this.partialSurrenderValidator = partialSurrenderValidator;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.featureRelationshipService = featureRelationshipService;
  }

  @GetMapping("/define-area")
  public ModelAndView renderDefineArea(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      @PathVariable UUID featureId
  ) {
    var commandJourneyId = partialSurrenderCorrectionService
        .getSurrenderDetailsOrThrow(licencePositionStagedPayload, featureId)
        .commandJourneyIdOrThrow();

    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourneyId);

    return getDefineAreaModelAndView(
        licencePositionStagedPayload,
        featureId,
        correction,
        commandJourneyId,
        activeFeatures
    );
  }

  @PostMapping("/define-area")
  public ModelAndView defineArea(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      @PathVariable UUID featureId
  ) {
    var commandJourneyId = partialSurrenderCorrectionService
        .getSurrenderDetailsOrThrow(licencePositionStagedPayload, featureId)
        .commandJourneyIdOrThrow();

    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourneyId);

    if (partialSurrenderDefineAreaValidator.hasErrors(activeFeatures)) {
      return getDefineAreaModelAndView(
          licencePositionStagedPayload,
          featureId,
          correction,
          commandJourneyId,
          activeFeatures
      )
          .addObject("errorSummaryItems",
              List.of(new ErrorSummaryItem(1, "split-area", NO_SPLIT_ERROR)))
          .addObject("mapErrorMessage", NO_SPLIT_ERROR);
    }

    partialSurrenderCorrectionService.clearSurrenderedIds(
        licencePositionStagedPayload,
        featureId,
        activeFeatures.stream().map(Feature::getId).toList()
    );

    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
        .renderSelectAreas(correction, licencePositionStagedPayload, featureId));
  }

  @GetMapping("/select-areas")
  public ModelAndView renderSelectAreas(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      @PathVariable UUID featureId
  ) {
    var surrenderDetails = partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(licencePositionStagedPayload, featureId);
    var activeFeatures = commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyIdOrThrow());

    return getSelectAreasModelAndView(
        licencePositionStagedPayload,
        featureId,
        correction,
        activeFeatures,
        PartialSurrenderSelectAreasForm.from(surrenderDetails)
    );
  }

  @PostMapping("/select-areas")
  public ModelAndView selectAreas(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      @PathVariable UUID featureId,
      @ModelAttribute("form") PartialSurrenderSelectAreasForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var surrenderDetails = partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(licencePositionStagedPayload, featureId);
    var activeFeatures = commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyIdOrThrow());

    var blockFeature = partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(
        licencePositionStagedPayload,
        featureId
    );

    if (partialSurrenderSelectAreasFormValidator.hasSelectionErrors(form, bindingResult, activeFeatures, blockFeature)
        || hasOverlapErrors(licencePositionStagedPayload, form, bindingResult, activeFeatures)) {
      return getSelectAreasModelAndView(
          licencePositionStagedPayload,
          featureId,
          correction,
          activeFeatures,
          form
      );
    }

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        licencePositionStagedPayload,
        featureId,
        form.getSurrenderedFeatureIds()
    );

    NotificationBanner.newSuccessBannerWithHeader("Areas to surrender saved", redirectAttributes);

    //TODO - EPGF-183: redirect to ended subareas when implemented
    if (partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(licencePositionStagedPayload).isPresent()) {
      return partialSurrenderCorrectionService.findCorrectedLiveChangeId(licencePositionStagedPayload)
          .map(changeId -> ReverseRouter.redirect(on(PartialSurrenderReviewAndSubmitController.class)
              .renderReviewAndSubmitForCorrectingChange(
                  correction,
                  licencePositionStagedPayload.getTargetLicencePosition(),
                  new LicencePositionChange(UUID.fromString(changeId)),
                  null)))
          .orElseGet(() -> ReverseRouter.redirect(on(PartialSurrenderReviewAndSubmitController.class)
              .renderReviewAndSubmit(correction, licencePositionStagedPayload, null)));
    }

    //TODO - EPGF-183: redirect to ended subareas when implemented
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, licencePositionStagedPayload, null)
    );
  }

  private boolean hasOverlapErrors(
      LicencePositionStagedPayload licencePositionStagedPayload,
      PartialSurrenderSelectAreasForm form,
      BindingResult bindingResult,
      List<Feature> activeFeatures
  ) {
    var otherActiveBlocks = partialSurrenderValidator.getOtherActiveBlocks(
        partialSurrenderCorrectionService.getCommittedPartialSurrenderOrThrow(licencePositionStagedPayload),
        licencePositionStagedPayloadService.resolveEffectiveDate(licencePositionStagedPayload),
        partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(licencePositionStagedPayload).orElse(null)
    );

    return partialSurrenderSelectAreasFormValidator.hasOverlapErrors(
        form,
        bindingResult,
        activeFeatures,
        featureRelationshipService.getLineBoundingBoxes(otherActiveBlocks)
    );
  }

  private ModelAndView getDefineAreaModelAndView(
      LicencePositionStagedPayload positionStagedPayload,
      UUID featureId,
      LicenceCorrection correction,
      UUID commandJourneyId,
      List<Feature> activeFeatures
  ) {
    var coordinateSystem = activeFeatures.getFirst().getCoordinateSystem();

    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderDefineArea")
        .addObject("commandJourneyId", commandJourneyId)
        .addObject("srsWkid", CoordinateSystemUtils.getWkid(coordinateSystem))
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("pageTitle", DEFINE_AREA_PAGE_TITLE)
        .addObject("backLinkUrl", defineAreaBackLinkUrl(positionStagedPayload, featureId));
  }

  private String defineAreaBackLinkUrl(
      LicencePositionStagedPayload positionStagedPayload,
      UUID featureId
  ) {
    if (partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionStagedPayload).isEmpty()) {
      return surrenderTypeUrl(positionStagedPayload, featureId);
    }

    return LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(positionStagedPayload);
  }

  /**
   * A surrender correcting an executed change is edited through the correcting-change route, so the back link points
   * there rather than at the staged form.
   */
  private String surrenderTypeUrl(
      LicencePositionStagedPayload positionStagedPayload,
      UUID featureId
  ) {
    return partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionStagedPayload)
        .map(changeId -> new LicencePositionChange(UUID.fromString(changeId)))
        .map(change -> ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeFormForCorrectingChange(
                positionStagedPayload.getLicenceCorrection(),
                positionStagedPayload.getTargetLicencePosition(),
                change,
                featureId)))
        .orElseGet(() -> ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(positionStagedPayload.getLicenceCorrection(), positionStagedPayload, featureId)));
  }

  private ModelAndView getSelectAreasModelAndView(
      LicencePositionStagedPayload licencePositionStagedPayload,
      UUID featureId,
      LicenceCorrection correction,
      List<Feature> activeFeatures,
      PartialSurrenderSelectAreasForm form
  ) {
    var coordinateSystem = activeFeatures.getFirst().getCoordinateSystem();
    var areaCheckboxOptions = LicenceBlockFeatureUtil.toBlockCheckboxOptions(activeFeatures);
    var activeFeatureIds = activeFeatures.stream().map(Feature::getId).toList();

    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderSelectAreas")
        .addObject("form", form)
        .addObject("pageTitle", SELECT_AREAS_PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("areaCheckboxOptions", areaCheckboxOptions)
        .addObject("activeFeatureIds", activeFeatureIds)
        .addObject("srsWkid", CoordinateSystemUtils.getWkid(coordinateSystem))
        .addObject("backLinkUrl", ReverseRouter.route(on(PartialSurrenderDefineAreaController.class)
            .renderDefineArea(correction, licencePositionStagedPayload, featureId)));
  }
}
