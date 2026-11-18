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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatus;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectionPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionHasStatus(LicenceCorrectionStatus.IN_PROGRESS)
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
  public ModelAndView renderForPosition(CorrectionPosition correctionPosition) {
    return getSubareaChangeModelAndView(
        correctionPosition.correction(),
        new SubareaChangeStartForm(),
        getBlockFeatures(correctionPosition),
        addChangeUrl(correctionPosition)
    );
  }

  @PostMapping("/position/{licencePositionId}/subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForPosition(
      CorrectionPosition correctionPosition,
      @ModelAttribute("form") SubareaChangeStartForm form,
      BindingResult bindingResult
  ) {
    var blockFeatures = getBlockFeatures(correctionPosition);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOn(correctionPosition);

    if (subareaChangeStartFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      return getSubareaChangeModelAndView(
          correctionPosition.correction(),
          form,
          blockFeatures,
          addChangeUrl(correctionPosition)
      );
    }

    var blockFeatureId = UUID.fromString(form.getFeatureId());
    var positionCorrection = subareaChangeService.commitSubareaChange(correctionPosition, blockFeatureId);

    return ReverseRouter.redirect(on(SubareaChangeController.class)
        .renderSubareaChange(correctionPosition.correction(), positionCorrection, blockFeatureId));
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

  private List<Feature> getBlockFeatures(CorrectionPosition correctionPosition) {
    return licencePositionSpatialService.getBlockFeaturesGoingIntoChange(
        correctionPosition.correction(),
        correctionPosition.positionId(),
        null
    );
  }

  private String addChangeUrl(CorrectionPosition correctionPosition) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class).renderForPosition(correctionPosition));
  }
}
