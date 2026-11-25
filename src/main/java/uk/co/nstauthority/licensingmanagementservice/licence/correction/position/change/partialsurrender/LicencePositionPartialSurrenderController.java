package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class LicencePositionPartialSurrenderController {

  private static final String PAGE_TITLE = "Surrender details";
  private static final String SAVED_BANNER = "Partial surrender details saved";
  private static final String CORRECTED_BANNER = "Partial surrender change corrected";

  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator;
  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;

  LicencePositionPartialSurrenderController(
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator,
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService
  ) {
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  @GetMapping("/position/{licencePositionId}/partial-surrender/surrender-details")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForPosition(CorrectionPosition correctionPosition) {
    var correction = correctionPosition.correction();
    var positionStagedPayload = licencePositionStagedPayloadService.findPositionStagedPayload(correctionPosition).orElse(null);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload).orElse(null);

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(existing),
        licencePositionStagedPayloadService.getEffectivePositionDate(correctionPosition),
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
            correction,
            correctionPosition.positionId(),
            partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionStagedPayload).orElse(null)),
        getBackLinkUrl(positionStagedPayload, existing, addChangeUrl(correctionPosition)));
  }

  @PostMapping("/position/{licencePositionId}/partial-surrender/surrender-details")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForPosition(
      CorrectionPosition correctionPosition,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var correction = correctionPosition.correction();
    var positionStagedPayload = licencePositionStagedPayloadService.findPositionStagedPayload(correctionPosition).orElse(null);
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(positionStagedPayload)
        .orElse(null);
    var blockFeatures = licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(correction, correctionPosition.positionId(), stagedChangeId);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload).orElse(null);
    var featureIdsAlreadyOperatedOn =
        licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOn(correctionPosition, stagedChangeId);

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionStagedPayloadService.getEffectivePositionDate(correctionPosition),
          blockFeatures,
          getBackLinkUrl(positionStagedPayload, existing, addChangeUrl(correctionPosition)));
    }

    var committedPositionStagedPayload = partialSurrenderCorrectionService.commitPartialSurrender(
        correctionPosition, toOperation(existing, form.getFeatureIds()));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, committedPositionStagedPayload, null));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/correct-surrender-details")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    var changeId = change.getId().toString();
    var positionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(correction, licencePosition)
        .orElse(null);
    var stagedSurrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload)
        .orElse(null);
    var singleBlock = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        correction,
        licencePosition,
        positionStagedPayload,
        changeId
    );

    if (singleBlock.isPresent()) {
      return startSingleBlockCorrectionModelAndView(correction, licencePosition, change, singleBlock.get());
    }

    var backLinkUrl = getBackLinkUrl(positionStagedPayload, stagedSurrender,
        correctingChangeTaskListUrl(correction, licencePosition, change));

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(getSurrenderToCorrect(stagedSurrender, changeId)),
        licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition),
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, changeId),
        backLinkUrl);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/correct-surrender-details/start")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView startCorrectingSingleBlockChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    var changeId = change.getId().toString();
    var singleBlockCorrection = partialSurrenderCorrectionService.stageSingleBlockCorrectionOfLiveChange(
        correction,
        licencePosition,
        changeId
    );

    if (singleBlockCorrection.isEmpty()) {
      throw new IllegalStateException("Change %s no longer has a single block to correct".formatted(changeId));
    }

    return redirectToDefineArea(
        singleBlockCorrection.get().licencePositionStagedPayload(),
        singleBlockCorrection.get().block()
    );
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/correct-surrender-details")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var changeId = change.getId().toString();
    var blockFeatures = licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, changeId);
    var positionStagedPayload = licencePositionStagedPayloadService
        .findUpdatePositionStagedPayload(correction, licencePosition)
        .orElse(null);
    var stagedSurrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionStagedPayload)
        .orElse(null);
    var featureIdsAlreadyOperatedOn = licencePositionStagedPayloadService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionStagedPayload,
        changeId
    );

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      var backLinkUrl = getBackLinkUrl(positionStagedPayload, stagedSurrender,
          correctingChangeTaskListUrl(correction, licencePosition, change));

      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionStagedPayloadService.getEffectivePositionDate(correction, licencePosition),
          blockFeatures,
          backLinkUrl);
    }

    var correctedSurrender = toOperation(getSurrenderToCorrect(stagedSurrender, changeId), form.getFeatureIds());
    var liveOperation = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId);
    if (correctedSurrender.hasUpdateOccurred(liveOperation)) {
      partialSurrenderCorrectionService.correctExistingPartialSurrender(
          correction,
          licencePosition,
          changeId,
          correctedSurrender);

      NotificationBanner.newSuccessBannerWithHeader(CORRECTED_BANNER, redirectAttributes);
    } else {
      partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
          correction,
          licencePosition,
          correctedSurrender
      );
    }

    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correction, licencePosition, change, null));
  }

  private PartialSurrenderOperation getSurrenderToCorrect(
      @Nullable PartialSurrenderOperation stagedSurrender,
      String changeId
  ) {
    return stagedSurrender != null ? stagedSurrender : partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId);
  }

  private String correctingChangeTaskListUrl(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correction, licencePosition, change, null));
  }

  private PartialSurrenderOperation toOperation(
      @Nullable PartialSurrenderOperation existing,
      Set<UUID> featureIds
  ) {
    var blockSurrendersByFeatureId = new HashMap<UUID, SurrenderDetails>();
    if (existing != null) {
      blockSurrendersByFeatureId.putAll(existing.featureIdToSurrenderDetails());
      blockSurrendersByFeatureId.keySet().retainAll(featureIds);
    }

    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(featureIds)
        .withSurrenderDetails(blockSurrendersByFeatureId)
        .build();
  }

  private ModelAndView surrenderDetailsModelAndView(
      LicenceCorrection correction,
      PartialSurrenderDetailsForm form,
      LocalDate surrenderDate,
      List<Feature> blockFeatures,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderDetails")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("surrenderDate", surrenderDate == null ? "" : DateUtil.formatLongDate(surrenderDate))
        .addObject("blockOptions", LicenceBlockFeatureUtil.toBlockCheckboxOptions(blockFeatures))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private String getBackLinkUrl(
      @Nullable LicencePositionStagedPayload positionStagedPayload,
      @Nullable PartialSurrenderOperation stagedSurrender,
      String addChangeUrl
  ) {
    if (positionStagedPayload == null || stagedSurrender == null) {
      return addChangeUrl;
    }

    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(positionStagedPayload.getLicenceCorrection(), positionStagedPayload, null));
  }

  private String addChangeUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class).renderForPosition(correctionPosition));
  }

  private ModelAndView startSingleBlockCorrectionModelAndView(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      Feature block
  ) {
    var liveSurrender = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(change.getId().toString());

    return new ModelAndView("lms/licence/correction/change/partialSurrender/startSingleBlockPartialSurrenderCorrection")
        .addObject("pageTitle", "Are you sure you want to correct this partial surrender?")
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("blockRows", partialSurrenderCorrectionService.getBlockRows(liveSurrender))
        .addObject("surrenderedFeatureIds", surrenderedFeatureIdsOf(liveSurrender, block))
        .addObject("srsWkid", CoordinateSystemUtils.getWkid(block.getCoordinateSystem()))
        .addObject("startCorrectionUrl", ReverseRouter.route(on(LicencePositionPartialSurrenderController.class)
            .startCorrectingSingleBlockChange(correction, licencePosition, change)))
        .addObject("backLinkUrl", LicencePositionStagedPayloadRouteUtil.getPositionPageUrl(correction, licencePosition.getId()));
  }

  private static List<UUID> surrenderedFeatureIdsOf(PartialSurrenderOperation surrender, Feature block) {
    return Optional.ofNullable(surrender.featureIdToSurrenderDetails().get(block.getId()))
        .map(SurrenderDetails::surrenderedFeatureIds)
        .filter(featureIds -> !featureIds.isEmpty())
        .orElseGet(() -> List.of(block.getId()));
  }

  private ModelAndView redirectToDefineArea(
      LicencePositionStagedPayload licencePositionStagedPayload,
      Feature block
  ) {
    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class).renderDefineArea(
        licencePositionStagedPayload.getLicenceCorrection(),
        licencePositionStagedPayload,
        block.getId()
    ));
  }
}
