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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
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
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class LicencePositionPartialSurrenderController {

  private static final String PAGE_TITLE = "Surrender details";
  private static final String SAVED_BANNER = "Partial surrender details saved";
  private static final String CORRECTED_BANNER = "Partial surrender change corrected";

  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionSpatialService licencePositionSpatialService;

  LicencePositionPartialSurrenderController(
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionSpatialService licencePositionSpatialService
  ) {
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionSpatialService = licencePositionSpatialService;
  }

  @GetMapping("/position/{licencePositionId}/partial-surrender/surrender-details")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(existing),
        licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
            correction,
            licencePosition,
            partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(positionCorrection).orElse(null)),
        getBackLinkUrl(correction, positionCorrection, existing, executedChangeUrl(correction, licencePosition)));
  }

  @PostMapping("/position/{licencePositionId}/partial-surrender/surrender-details")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(positionCorrection)
        .orElse(null);
    var blockFeatures = licencePositionSpatialService
        .getBlockFeaturesGoingIntoChange(correction, licencePosition, stagedChangeId);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionCorrection,
        stagedChangeId
    );

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
          blockFeatures,
          getBackLinkUrl(correction, positionCorrection, existing, executedChangeUrl(correction, licencePosition)));
    }

    var committedPositionCorrection = partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        correction, licencePosition, toOperation(existing, form.getFeatureIds()));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, committedPositionCorrection, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/partial-surrender/surrender-details")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection).orElse(null);

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(existing),
        licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection),
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
            licencePositionCorrection,
            partialSurrenderCorrectionService.getCommittedPartialSurrenderChangeId(licencePositionCorrection).orElse(null)),
        getBackLinkUrl(
            correction, licencePositionCorrection, existing, addedChangeUrl(correction, licencePositionCorrection)));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/partial-surrender/surrender-details")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var stagedChangeId = partialSurrenderCorrectionService
        .getCommittedPartialSurrenderChangeId(licencePositionCorrection)
        .orElse(null);
    var blockFeatures = licencePositionSpatialService.getBlockFeaturesGoingIntoChange(licencePositionCorrection, stagedChangeId);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionCorrection).orElse(null);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        licencePositionCorrection,
        stagedChangeId
    );

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionCorrectionService.resolveEffectiveDate(licencePositionCorrection),
          blockFeatures,
          getBackLinkUrl(correction, licencePositionCorrection, existing,
              addedChangeUrl(correction, licencePositionCorrection)));
    }

    partialSurrenderCorrectionService.commitPartialSurrender(
        licencePositionCorrection, toOperation(existing, form.getFeatureIds()));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, licencePositionCorrection, null));
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
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var stagedSurrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)
        .orElse(null);
    var singleBlock = partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(
        correction,
        licencePosition,
        positionCorrection,
        changeId
    );

    if (singleBlock.isPresent()) {
      return startSingleBlockCorrectionModelAndView(correction, licencePosition, change, singleBlock.get());
    }

    var backLinkUrl = getBackLinkUrl(correction, positionCorrection, stagedSurrender,
        correctingChangeTaskListUrl(correction, licencePosition, change));

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(getSurrenderToCorrect(stagedSurrender, changeId)),
        licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
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
        correction,
        singleBlockCorrection.get().licencePositionCorrection(),
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
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var stagedSurrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)
        .orElse(null);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionCorrection,
        changeId
    );

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      var backLinkUrl = getBackLinkUrl(correction, positionCorrection, stagedSurrender,
          correctingChangeTaskListUrl(correction, licencePosition, change));

      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
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
      LicenceCorrection correction,
      @Nullable LicencePositionCorrection positionCorrection,
      @Nullable PartialSurrenderOperation stagedSurrender,
      String addChangeUrl
  ) {
    if (positionCorrection == null || stagedSurrender == null) {
      return addChangeUrl;
    }

    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, positionCorrection, null));
  }

  private String executedChangeUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(correction, licencePosition));
  }

  private String addedChangeUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correction, licencePositionCorrection));
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
        .addObject("backLinkUrl", ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderLicencePosition(correction, licencePosition)));
  }

  private static List<UUID> surrenderedFeatureIdsOf(PartialSurrenderOperation surrender, Feature block) {
    return Optional.ofNullable(surrender.featureIdToSurrenderDetails().get(block.getId()))
        .map(SurrenderDetails::surrenderedFeatureIds)
        .filter(featureIds -> !featureIds.isEmpty())
        .orElseGet(() -> List.of(block.getId()));
  }

  private ModelAndView redirectToDefineArea(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      Feature block
  ) {
    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class).renderDefineArea(
        correction,
        licencePositionCorrection,
        block.getId()
    ));
  }
}
