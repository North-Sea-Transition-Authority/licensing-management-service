package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.position.LicencePositionBelongsToCorrectionLicence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class LicencePositionSubareaChangeStartController {

  private static final String PAGE_TITLE = "Subarea change";

  private final LicencePositionSpatialService licencePositionSpatialService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final SubareaChangeService subareaChangeService;
  private final SubareaChangeStartFormValidator subareaChangeStartFormValidator;

  public LicencePositionSubareaChangeStartController(
      LicencePositionSpatialService licencePositionSpatialService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      SubareaChangeService subareaChangeService,
      SubareaChangeStartFormValidator subareaChangeStartFormValidator
  ) {
    this.licencePositionSpatialService = licencePositionSpatialService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.subareaChangeService = subareaChangeService;
    this.subareaChangeStartFormValidator = subareaChangeStartFormValidator;
  }

  @GetMapping("/position/{licencePositionId}/subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView renderForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition
  ) {
    return getSubareaChangeModelAndView(
        correction,
        new SubareaChangeStartForm(),
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, null),
        executedChangeUrl(correction, licencePosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  @LicencePositionBelongsToCorrectionLicence
  public ModelAndView submitForExecutedPosition(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      @ModelAttribute("form") SubareaChangeStartForm form,
      BindingResult bindingResult
  ) {
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var blockFeatures = licencePositionSpatialService.getBlockFeaturesGoingIntoChange(correction, licencePosition, null);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
        correction,
        licencePosition,
        positionCorrection
    );

    if (subareaChangeStartFormValidator.hasErrors(
        form,
        bindingResult,
        blockFeatures,
        featureIdsAlreadyOperatedOn
    )) {
      return getSubareaChangeModelAndView(
          correction,
          form,
          blockFeatures,
          executedChangeUrl(correction, licencePosition)
      );
    }

    var blockFeatureId = UUID.fromString(form.getFeatureId());
    var savedPositionCorrection = subareaChangeService.commitSubareaChangeForExecutedPosition(
        correction,
        licencePosition,
        blockFeatureId
    );

    return ReverseRouter.redirect(on(SubareaChangeController.class)
        .renderSubareaChange(correction, savedPositionCorrection, blockFeatureId));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/subarea-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView renderForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection
  ) {
    return getSubareaChangeModelAndView(
        correction,
        new SubareaChangeStartForm(),
        licencePositionSpatialService.getBlockFeaturesGoingIntoChange(licencePositionCorrection, null),
        addedChangeUrl(correction, licencePositionCorrection)
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/subarea-change")
  @LicencePositionCorrectionBelongsToCorrection
  public ModelAndView submitForAddedPosition(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @ModelAttribute("form") SubareaChangeStartForm form,
      BindingResult bindingResult
  ) {
    var blockFeatures = licencePositionSpatialService.getBlockFeaturesGoingIntoChange(licencePositionCorrection, null);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        licencePositionCorrection
    );

    if (subareaChangeStartFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      return getSubareaChangeModelAndView(
          correction,
          form,
          blockFeatures,
          addedChangeUrl(correction, licencePositionCorrection)
      );
    }

    var blockFeatureId = UUID.fromString(form.getFeatureId());
    subareaChangeService.commitSubareaChange(licencePositionCorrection, blockFeatureId);

    return ReverseRouter.redirect(on(SubareaChangeController.class)
        .renderSubareaChange(correction, licencePositionCorrection, blockFeatureId));
  }

  private ModelAndView getSubareaChangeModelAndView(
      LicenceCorrection correction,
      SubareaChangeStartForm form,
      List<Feature> blockFeatures,
      String backLinkUrl
  ) {
    return new ModelAndView("lms/licence/correction/change/subarea/startSubareaChange")
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("pageCaption", correction.getLicence().getLicenceReference())
        .addObject("form", form)
        .addObject("blockOptions", LicenceBlockFeatureUtil.toBlockCheckboxOptions(blockFeatures))
        .addObject("backLinkUrl", backLinkUrl);
  }

  private String executedChangeUrl(LicenceCorrection correction, LicencePosition licencePosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(correction, licencePosition));
  }

  private String addedChangeUrl(LicenceCorrection correction, LicencePositionCorrection licencePositionCorrection) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correction, licencePositionCorrection));
  }
}
