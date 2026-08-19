package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

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
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "/licence-corrections/{correctionId}/position-correction" +
    "/{licencePositionCorrectionId}/partial-surrender/block/{featureId}"
)
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class BlockSurrenderTypeController {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator;

  public BlockSurrenderTypeController(
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      BlockSurrenderTypeFormValidator blockSurrenderTypeFormValidator
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.blockSurrenderTypeFormValidator = blockSurrenderTypeFormValidator;
  }

  @GetMapping("/surrender-type")
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

  @PostMapping("/surrender-type")
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

    NotificationBanner.newSuccessBannerWithHeader("Partial surrender type saved", redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null));
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
}
