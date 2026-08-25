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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeBelongsToPosition;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.change.LicencePositionChangeIsOfType;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderDefineAreaController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class BlockSurrenderTypeController {

  private static final String SAVED_BANNER = "Partial surrender type saved";

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionService licencePositionService;
  private final BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;

  public BlockSurrenderTypeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionService licencePositionService,
      BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionService = licencePositionService;
    this.blockSurrenderTypeFormValidator = blockSurrenderTypeFormValidator;
  }

  @GetMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/block/{featureId}/surrender-type")
  public ModelAndView renderSurrenderTypeForm(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var feature = partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, featureId);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);

    return surrenderTypeModelAndView(
        correction,
        feature,
        BlockSurrenderTypeForm.from(existing, featureId),
        taskListUrl(correctionId, licencePositionCorrectionId)
    );
  }

  @PostMapping("/position-correction/{licencePositionCorrectionId}/partial-surrender/block/{featureId}/surrender-type")
  public ModelAndView submitSurrenderTypeForm(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") BlockSurrenderTypeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var feature = partialSurrenderCorrectionService.getSurrenderedBlockFeatureOrThrow(positionCorrection, featureId);

    if (blockSurrenderTypeFormValidator.hasErrors(form, bindingResult)) {
      return surrenderTypeModelAndView(
          correction,
          feature,
          form,
          taskListUrl(correctionId, licencePositionCorrectionId)
      );
    }

    partialSurrenderCorrectionService.setBlockSurrenderType(
        positionCorrection,
        featureId,
        BlockSurrenderType.valueOf(form.getSurrenderType())
    );

    if (Objects.equals(form.getSurrenderType(), BlockSurrenderType.PARTIAL_SURRENDER.getEnumName())) {
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correctionId, licencePositionCorrectionId, featureId, null));
    }

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/block/{featureId}/correct-surrender-type")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView renderSurrenderTypeFormForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var surrenderUnderCorrection = partialSurrenderCorrectionService
        .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId);
    var feature = partialSurrenderCorrectionService
        .getSurrenderedBlockFeatureOrThrow(surrenderUnderCorrection, featureId);

    return surrenderTypeModelAndView(
        correction,
        feature,
        BlockSurrenderTypeForm.from(surrenderUnderCorrection, featureId),
        correctingChangeTaskListUrl(correctionId, licencePositionId, changeId)
    );
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/block/{featureId}/correct-surrender-type")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView submitSurrenderTypeFormForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") BlockSurrenderTypeForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var surrenderUnderCorrection = partialSurrenderCorrectionService
        .getSurrenderUnderCorrectionOrThrow(correction, licencePosition, changeId);
    var feature = partialSurrenderCorrectionService
        .getSurrenderedBlockFeatureOrThrow(surrenderUnderCorrection, featureId);

    if (blockSurrenderTypeFormValidator.hasErrors(form, bindingResult)) {
      return surrenderTypeModelAndView(
          correction,
          feature,
          form,
          correctingChangeTaskListUrl(correctionId, licencePositionId, changeId)
      );
    }

    var blockSurrenderType = BlockSurrenderType.valueOf(form.getSurrenderType());

    // the surrender date is deliberately omitted so that saving a type does not stage a date correction; the block's
    // existing command journey is reused (a block can only carry one) so the correction points at the same splits
    var correctedSurrender = partialSurrenderCorrectionService
        .getOrCreatePartialSurrenderDetails(surrenderUnderCorrection, featureId, blockSurrenderType);

    if (blockSurrenderType == BlockSurrenderType.PARTIAL_SURRENDER) {
      var positionCorrection = partialSurrenderCorrectionService
          .correctExistingPartialSurrender(correction, licencePosition, changeId, correctedSurrender);
      return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
          .renderDefineArea(correctionId, positionCorrection.getId(), featureId, null));
    }

    if (correctedSurrender.hasUpdateOccurred(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId))) {
      partialSurrenderCorrectionService.correctExistingPartialSurrender(
          correction,
          licencePosition,
          changeId,
          correctedSurrender);
    } else {
      partialSurrenderCorrectionService.revertPartialSurrenderCorrection(correction, licencePosition);
    }

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correctionId, licencePositionId, changeId, null, null));
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

  private String taskListUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null));
  }

  private String correctingChangeTaskListUrl(UUID correctionId, UUID licencePositionId, String changeId) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correctionId, licencePositionId, changeId, null, null));
  }
}
