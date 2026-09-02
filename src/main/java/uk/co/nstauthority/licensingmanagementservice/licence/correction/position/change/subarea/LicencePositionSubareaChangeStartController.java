package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionIsNotRemovedInCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class LicencePositionSubareaChangeStartController {

  private static final String PAGE_TITLE = "Subarea change";
  private static final String SAVED_BANNER = "Subarea change saved";

  private final LicencePositionService licencePositionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final SubareaChangeService subareaChangeService;
  private final SubareaChangeStartFormValidator subareaChangeStartFormValidator;

  public LicencePositionSubareaChangeStartController(
      LicencePositionService licencePositionService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      SubareaChangeService subareaChangeService,
      SubareaChangeStartFormValidator subareaChangeStartFormValidator
  ) {
    this.licencePositionService = licencePositionService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.subareaChangeService = subareaChangeService;
    this.subareaChangeStartFormValidator = subareaChangeStartFormValidator;
  }

  @GetMapping("/position/{licencePositionId}/subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);

    return getSubareaChangeModelAndView(
        correction,
        new SubareaChangeStartForm(),
        //TODO - EPGF-239: The features available to use will depend on the output of previous changes
        licencePositionService.getBlockFeatures(licencePosition),
        executedChangeUrl(correctionId, licencePositionId)
    );
  }

  @PostMapping("/position/{licencePositionId}/subarea-change")
  @LicencePositionIsNotRemovedInCorrection
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") SubareaChangeStartForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    //TODO - EPGF-239: The features available to use will depend on the output of previous changes
    var blockFeatures = licencePositionService.getBlockFeatures(licencePosition);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForExecutedPosition(
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
          executedChangeUrl(correctionId, licencePositionId)
      );
    }

    subareaChangeService.commitSubareaChangeForExecutedPosition(correction, licencePosition, toOperation(form));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderLicencePosition(correctionId, licencePositionId, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/subarea-change")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);

    return getSubareaChangeModelAndView(
        correction,
        new SubareaChangeStartForm(),
        //TODO - EPGF-239: The features available to use will depend on the output of previous changes
        licencePositionService.getBlockFeaturesForCorrection(positionCorrection),
        addedChangeUrl(correctionId, licencePositionCorrectionId)
    );
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/subarea-change")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") SubareaChangeStartForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    //TODO - EPGF-239: The features available to use will depend on the output of previous changes
    var blockFeatures = licencePositionService.getBlockFeaturesForCorrection(positionCorrection);
    var featureIdsAlreadyOperatedOn = licencePositionCorrectionService.blockFeatureIdsAlreadyOperatedOnForAddedPosition(
        positionCorrection
    );

    if (subareaChangeStartFormValidator.hasErrors(form, bindingResult, blockFeatures, featureIdsAlreadyOperatedOn)) {
      return getSubareaChangeModelAndView(
          correction,
          form,
          blockFeatures,
          addedChangeUrl(correctionId, licencePositionCorrectionId)
      );
    }

    subareaChangeService.commitSubareaChange(positionCorrection, toOperation(form));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(LicenceCorrectionController.class)
        .renderAddedPosition(correctionId, licencePositionCorrectionId, null));
  }

  private SubareaOperation toOperation(SubareaChangeStartForm form) {
    return LicenceOperation.newSubAreaOperation()
        .withFeatureId(UUID.fromString(form.getFeatureId()))
        .build();
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

  private String executedChangeUrl(UUID correctionId, UUID licencePositionId) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForExecutedPosition(correctionId, licencePositionId, null));
  }

  private String addedChangeUrl(UUID correctionId, UUID licencePositionCorrectionId) {
    return ReverseRouter.route(on(LicencePositionAddChangeController.class)
        .renderForAddedPosition(correctionId, licencePositionCorrectionId, null));
  }
}
