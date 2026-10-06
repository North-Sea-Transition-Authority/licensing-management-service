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
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.LicencePositionCorrectionBelongsToCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionRouteUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "/licence-corrections/{correctionId}/position-correction" +
        "/{licencePositionCorrectionId}/partial-surrender/{featureId}"
)
@InvokingUserCanViewCorrection
@LicencePositionCorrectionBelongsToCorrection
@CorrectionLicenceIsType({LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderDefineAreaController {

  private static final String DEFINE_AREA_PAGE_TITLE = "Define area to surrender";
  private static final String SELECT_AREAS_PAGE_TITLE = "Select the areas to surrender";
  private static final String NO_SPLIT_ERROR = "You must split the block before continuing";

  private final CommandJourneyService commandJourneyService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;
  private final PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator;

  public PartialSurrenderDefineAreaController(
      CommandJourneyService commandJourneyService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator,
      PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator
  ) {
    this.commandJourneyService = commandJourneyService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.partialSurrenderSelectAreasFormValidator = partialSurrenderSelectAreasFormValidator;
    this.partialSurrenderDefineAreaValidator = partialSurrenderDefineAreaValidator;
  }

  @GetMapping("/define-area")
  public ModelAndView renderDefineArea(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID featureId
  ) {
    var commandJourneyId = partialSurrenderCorrectionService
        .getSurrenderDetailsOrThrow(licencePositionCorrection, featureId)
        .commandJourneyIdOrThrow();

    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourneyId);

    return getDefineAreaModelAndView(
        licencePositionCorrection,
        featureId,
        correction,
        commandJourneyId,
        activeFeatures
    );
  }

  @PostMapping("/define-area")
  public ModelAndView defineArea(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID featureId
  ) {
    var commandJourneyId = partialSurrenderCorrectionService
        .getSurrenderDetailsOrThrow(licencePositionCorrection, featureId)
        .commandJourneyIdOrThrow();

    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourneyId);

    if (partialSurrenderDefineAreaValidator.hasErrors(activeFeatures)) {
      return getDefineAreaModelAndView(
          licencePositionCorrection,
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
        licencePositionCorrection,
        featureId,
        activeFeatures.stream().map(Feature::getId).toList()
    );

    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
        .renderSelectAreas(correction, licencePositionCorrection, featureId));
  }

  @GetMapping("/select-areas")
  public ModelAndView renderSelectAreas(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID featureId
  ) {
    var surrenderDetails = partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(licencePositionCorrection, featureId);
    var activeFeatures = commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyIdOrThrow());

    return getSelectAreasModelAndView(
        licencePositionCorrection,
        featureId,
        correction,
        activeFeatures,
        PartialSurrenderSelectAreasForm.from(surrenderDetails)
    );
  }

  @PostMapping("/select-areas")
  public ModelAndView selectAreas(
      LicenceCorrection correction,
      LicencePositionCorrection licencePositionCorrection,
      @PathVariable UUID featureId,
      @ModelAttribute("form") PartialSurrenderSelectAreasForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var surrenderDetails = partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(licencePositionCorrection, featureId);
    var activeFeatures = commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyIdOrThrow());

    if (partialSurrenderSelectAreasFormValidator.hasErrors(form, bindingResult, activeFeatures)) {
      return getSelectAreasModelAndView(
          licencePositionCorrection,
          featureId,
          correction,
          activeFeatures,
          form
      );
    }

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        licencePositionCorrection,
        featureId,
        form.getSurrenderedFeatureIds()
    );

    NotificationBanner.newSuccessBannerWithHeader("Areas to surrender saved", redirectAttributes);

    //TODO - EPGF-183: redirect to ended subareas when implemented
    if (partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(licencePositionCorrection).isPresent()) {
      return partialSurrenderCorrectionService.findCorrectedLiveChangeId(licencePositionCorrection)
          .map(changeId -> ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
              .renderReviewAndSubmitForCorrectingChange(
                  correction,
                  licencePositionCorrection.getTargetLicencePosition(),
                  new LicencePositionChange(UUID.fromString(changeId)),
                  null)))
          .orElseGet(() -> ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
              .renderReviewAndSubmit(correction, licencePositionCorrection, null)));
    }

    //TODO - EPGF-183: redirect to ended subareas when implemented
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correction, licencePositionCorrection, null)
    );
  }

  private ModelAndView getDefineAreaModelAndView(
      LicencePositionCorrection positionCorrection,
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
        .addObject("backLinkUrl", defineAreaBackLinkUrl(correction, positionCorrection, featureId));
  }

  private String defineAreaBackLinkUrl(
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection,
      UUID featureId
  ) {
    if (partialSurrenderCorrectionService.findSingleBlockNotOperatedOn(positionCorrection).isEmpty()) {
      return surrenderTypeUrl(correction, positionCorrection, featureId);
    }

    if (partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection).isPresent()) {
      return ReverseRouter.route(on(LicenceCorrectionController.class)
          .renderLicencePosition(correction, positionCorrection.getTargetLicencePosition()));
    }

    return LicencePositionCorrectionRouteUtil.getPositionPageUrl(correction, positionCorrection);
  }

  /**
   * A surrender correcting an executed change is edited through the correcting-change route, so the back link points
   * there rather than at the staged form.
   */
  private String surrenderTypeUrl(
      LicenceCorrection correction,
      LicencePositionCorrection positionCorrection,
      UUID featureId
  ) {
    return partialSurrenderCorrectionService.findCorrectedLiveChangeId(positionCorrection)
        .map(changeId -> new LicencePositionChange(UUID.fromString(changeId)))
        .map(change -> ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeFormForCorrectingChange(
                correction,
                positionCorrection.getTargetLicencePosition(),
                change,
                featureId)))
        .orElseGet(() -> ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(correction, positionCorrection, featureId)));
  }

  private ModelAndView getSelectAreasModelAndView(
      LicencePositionCorrection licencePositionCorrection,
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
            .renderDefineArea(correction, licencePositionCorrection, featureId)));
  }
}
