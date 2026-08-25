package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.LicencePositionAddChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.DateUtil;

@Controller
@RequestMapping("/licence-corrections/{correctionId}")
@Profile("enable-lms2")
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class LicencePositionPartialSurrenderController {

  private static final String PAGE_TITLE = "Surrender details";
  private static final String SAVED_BANNER = "Partial surrender details saved";
  private static final String CORRECTED_BANNER = "Partial surrender change corrected";

  private final PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionService licencePositionService;

  LicencePositionPartialSurrenderController(
      PartialSurrenderDetailsFormValidator partialSurrenderDetailsFormValidator,
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionService licencePositionService
  ) {
    this.partialSurrenderDetailsFormValidator = partialSurrenderDetailsFormValidator;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionService = licencePositionService;
  }

  @GetMapping("/position/{licencePositionId}/partial-surrender/surrender-details")
  public ModelAndView renderForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(existing),
        licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
        licencePositionService.getBlockFeatures(licencePosition),
        getBackLinkUrl(correctionId, positionCorrection, existing, executedChangeUrl(correctionId, licencePositionId)));
  }

  @PostMapping("/position/{licencePositionId}/partial-surrender/surrender-details")
  public ModelAndView submitForExecutedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var blockFeatures = licencePositionService.getBlockFeatures(licencePosition);
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures)) {
      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
          blockFeatures,
          getBackLinkUrl(correctionId, positionCorrection, existing, executedChangeUrl(correctionId, licencePositionId)));
    }

    var committedPositionCorrection = partialSurrenderCorrectionService.commitPartialSurrenderForExecutedPosition(
        correction, licencePosition, toOperation(existing, form));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, committedPositionCorrection.getId(), null, null));
  }

  @GetMapping("/added-position/{licencePositionCorrectionId}/partial-surrender/surrender-details")
  public ModelAndView renderForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(existing),
        licencePositionCorrectionService.resolveEffectiveDate(positionCorrection),
        partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection),
        getBackLinkUrl(correctionId, positionCorrection, existing, addedChangeUrl(correctionId, licencePositionCorrectionId)));
  }

  @PostMapping("/added-position/{licencePositionCorrectionId}/partial-surrender/surrender-details")
  public ModelAndView submitForAddedPosition(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var blockFeatures = partialSurrenderCorrectionService.getSurrenderableBlockFeatures(positionCorrection);
    var existing = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection).orElse(null);

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures)) {
      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionCorrectionService.resolveEffectiveDate(positionCorrection),
          blockFeatures,
          getBackLinkUrl(correctionId, positionCorrection, existing,
              addedChangeUrl(correctionId, licencePositionCorrectionId)));
    }

    partialSurrenderCorrectionService.commitPartialSurrender(positionCorrection, toOperation(existing, form));

    NotificationBanner.newSuccessBannerWithHeader(SAVED_BANNER, redirectAttributes);
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null));
  }

  @GetMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/correct-surrender-details")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView renderForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var stagedSurrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)
        .orElse(null);
    var backLinkUrl = getBackLinkUrl(correctionId, positionCorrection, stagedSurrender,
        correctingChangeTaskListUrl(correctionId, licencePositionId, changeId));

    return surrenderDetailsModelAndView(
        correction,
        PartialSurrenderDetailsForm.from(getSurrenderToCorrect(stagedSurrender, changeId)),
        licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
        licencePositionService.getBlockFeatures(licencePosition),
        backLinkUrl);
  }

  @PostMapping("/position/{licencePositionId}/change/{changeId}/partial-surrender/correct-surrender-details")
  @LicencePositionChangeBelongsToPosition
  @LicencePositionChangeIsOfType(PartialSurrenderOperation.class)
  public ModelAndView submitForCorrectingChange(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionId,
      @PathVariable String changeId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") PartialSurrenderDetailsForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var licencePosition = licencePositionService.getPositionForLicence(correction.getLicence(), licencePositionId);
    var blockFeatures = licencePositionService.getBlockFeatures(licencePosition);
    var positionCorrection = licencePositionCorrectionService
        .findUpdatePositionCorrection(correction, licencePosition)
        .orElse(null);
    var stagedSurrender = partialSurrenderCorrectionService.getCommittedPartialSurrender(positionCorrection)
        .orElse(null);

    if (partialSurrenderDetailsFormValidator.hasErrors(form, bindingResult, blockFeatures)) {
      var backLinkUrl = getBackLinkUrl(correctionId, positionCorrection, stagedSurrender,
          correctingChangeTaskListUrl(correctionId, licencePositionId, changeId));

      return surrenderDetailsModelAndView(
          correction,
          form,
          licencePositionCorrectionService.getEffectivePositionDate(correction, licencePosition),
          blockFeatures,
          backLinkUrl);
    }

    var correctedSurrender = toOperation(getSurrenderToCorrect(stagedSurrender, changeId), form);

    if (correctedSurrender.hasUpdateOccurred(partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId))) {
      partialSurrenderCorrectionService.correctExistingPartialSurrender(
          correction,
          licencePosition,
          changeId,
          correctedSurrender);

      NotificationBanner.newSuccessBannerWithHeader(CORRECTED_BANNER, redirectAttributes);
    } else {
      partialSurrenderCorrectionService.revertPartialSurrenderCorrection(correction, licencePosition);
    }

    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correctionId, licencePositionId, changeId, null, null));
  }

  private PartialSurrenderOperation getSurrenderToCorrect(
      @Nullable PartialSurrenderOperation stagedSurrender,
      String changeId
  ) {
    return stagedSurrender != null ? stagedSurrender : partialSurrenderCorrectionService.getLiveSurrenderOrThrow(changeId);
  }

  private String correctingChangeTaskListUrl(UUID correctionId, UUID licencePositionId, String changeId) {
    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderForCorrectingChange(correctionId, licencePositionId, changeId, null, null));
  }

  private PartialSurrenderOperation toOperation(
      @Nullable PartialSurrenderOperation existing,
      PartialSurrenderDetailsForm form
  ) {
    var blockSurrendersByFeatureId = new HashMap<UUID, SurrenderDetails>();
    if (existing != null) {
      blockSurrendersByFeatureId.putAll(existing.featureIdToSurrenderDetails());
      blockSurrendersByFeatureId.keySet().retainAll(form.getFeatureIds());
    }

    return LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(form.getFeatureIds())
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
      UUID correctionId,
      @Nullable LicencePositionCorrection positionCorrection,
      @Nullable PartialSurrenderOperation stagedSurrender,
      String addChangeUrl
  ) {
    if (positionCorrection == null || stagedSurrender == null) {
      return addChangeUrl;
    }

    return ReverseRouter.route(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, positionCorrection.getId(), null, null));
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
