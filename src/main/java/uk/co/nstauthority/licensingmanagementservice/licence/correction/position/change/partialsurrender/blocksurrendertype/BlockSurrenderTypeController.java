package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Objects;
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
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionStagedPayloadBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class BlockSurrenderTypeController {

  private static final String SAVED_BANNER = "Partial surrender type saved";

  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;

  public BlockSurrenderTypeController(
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator
  ) {
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.blockSurrenderTypeFormValidator = blockSurrenderTypeFormValidator;
  }

  @GetMapping("/position-staged-payload/{licencePositionStagedPayloadId}/partial-surrender/block/{featureId}/surrender-type")
  @LicencePositionStagedPayloadBelongsToCorrection
  public ModelAndView renderSurrenderTypeForm(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      @PathVariable UUID featureId
  ) {
    var feature = partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(licencePositionStagedPayload, featureId);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(licencePositionStagedPayload).orElse(null);

    return surrenderTypeModelAndView(
        correction,
        feature,
        BlockSurrenderTypeForm.from(existing, featureId),
        taskListUrl(licencePositionStagedPayload)
    );
  }

  @PostMapping("/position-staged-payload/{licencePositionStagedPayloadId}/partial-surrender/block/{featureId}/surrender-type")
  @LicencePositionStagedPayloadBelongsToCorrection
  public ModelAndView submitSurrenderTypeForm(
      LicenceCorrection correction,
      LicencePositionStagedPayload licencePositionStagedPayload,
      @PathVariable UUID featureId,
      @ModelAttribute("form") BlockSurrenderTypeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var feature = partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(licencePositionStagedPayload, featureId);

    if (blockSurrenderTypeFormValidator.hasErrors(form, bindingResult)) {
      return surrenderTypeModelAndView(
          correction,
          feature,
          form,
          taskListUrl(licencePositionStagedPayload)
      );
    }

    partialSurrenderCorrectionService.setBlockSurrenderType(
        licencePositionStagedPayload,
        featureId,
        BlockSurrenderType.valueOf(form.getSurrenderType())
    );

    if (Objects.equals(form.getSurrenderType(), BlockSurrenderType.PARTIAL_SURRENDER.getEnumName())) {
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correction, licencePositionStagedPayload, featureId));
    }

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, licencePositionStagedPayload, null));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/block/{featureId}/correct-surrender-type")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderSurrenderTypeFormForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      @PathVariable UUID featureId
  ) {
    var surrenderUnderCorrection = partialSurrenderCorrectionService
        .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, change.getId().toString());
    var feature = partialSurrenderCorrectionService
        .getSurrenderedBlockFeatureOrThrow(surrenderUnderCorrection, featureId);

    return surrenderTypeModelAndView(
        correction,
        feature,
        BlockSurrenderTypeForm.from(surrenderUnderCorrection, featureId),
        correctingChangeTaskListUrl(correction, licencePosition, change)
    );
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/block/{featureId}/correct-surrender-type")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitSurrenderTypeFormForCorrectingChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change,
      @PathVariable UUID featureId,
      @ModelAttribute("form") BlockSurrenderTypeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var changeId = change.getId().toString();
    var surrenderUnderCorrection = partialSurrenderCorrectionService
        .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId);
    var feature = partialSurrenderCorrectionService
        .getSurrenderedBlockFeatureOrThrow(surrenderUnderCorrection, featureId);

    if (blockSurrenderTypeFormValidator.hasErrors(form, bindingResult)) {
      return surrenderTypeModelAndView(
          correction,
          feature,
          form,
          correctingChangeTaskListUrl(correction, licencePosition, change)
      );
    }

    var blockSurrenderType = BlockSurrenderType.valueOf(form.getSurrenderType());
    var liveOperation = partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId);

    // the surrender date is deliberately omitted so that saving a type does not stage a date correction
    var correctedSurrender = partialSurrenderCorrectionService
        .getOrCreatePartialSurrenderDetails(surrenderUnderCorrection, featureId, blockSurrenderType);

    if (blockSurrenderType == BlockSurrenderType.PARTIAL_SURRENDER) {
      var positionStagedPayload = partialSurrenderCorrectionService
          .correctExistingPartialSurrender(correction, licencePosition, changeId, correctedSurrender);
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correction, positionStagedPayload, featureId));
    }

    if (correctedSurrender.hasUpdateOccurred(liveOperation)) {
      partialSurrenderCorrectionService.correctExistingPartialSurrender(
          correction,
          licencePosition,
          changeId,
          correctedSurrender
      );
    } else {
      partialSurrenderCorrectionService.revertPartialSurrenderCorrection(
          correction,
          licencePosition,
          correctedSurrender
      );
    }

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correction, licencePosition, change, null));
  }

  private ModelAndView surrenderTypeModelAndView(
      LicenceCorrection correction,
      Feature feature,
      BlockSurrenderTypeForm form,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/change/partialSurrender/partialSurrenderType")
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("blockName", "Block %s".formatted(feature.getFeatureName()))
        .addObject("form", form)
        .addObject("surrenderTypeOptions", BlockSurrenderType.getOptions())
        .addObject("backLinkUrl", backLinkUrl);
  }

  private String taskListUrl(LicencePositionStagedPayload licencePositionStagedPayload) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(licencePositionStagedPayload.getLicenceCorrection(), licencePositionStagedPayload, null));
  }

  private String correctingChangeTaskListUrl(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      LicencePositionChange change
  ) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correction, licencePosition, change, null));
  }
}
