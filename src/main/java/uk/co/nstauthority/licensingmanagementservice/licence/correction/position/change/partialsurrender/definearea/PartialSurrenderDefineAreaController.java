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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "/licence-corrections/{correctionId}/position-correction" +
        "/{licencePositionCorrectionId}/partial-surrender/{featureId}"
)
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderDefineAreaController {

  private static final String DEFINE_AREA_PAGE_TITLE = "Define area to surrender";
  private static final String SELECT_AREAS_PAGE_TITLE = "Select the areas to surrender";
  private static final String NO_SPLIT_ERROR = "You must split the block before continuing";

  private final CommandJourneyService commandJourneyService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;
  private final PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator;

  public PartialSurrenderDefineAreaController(
      CommandJourneyService commandJourneyService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator,
      PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator
  ) {
    this.commandJourneyService = commandJourneyService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.partialSurrenderSelectAreasFormValidator = partialSurrenderSelectAreasFormValidator;
    this.partialSurrenderDefineAreaValidator = partialSurrenderDefineAreaValidator;
  }

  @GetMapping("/define-area")
  public ModelAndView renderDefineArea(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);

    var commandJourneyId = partialSurrenderCorrectionService
        .getSurrenderDetailsOrThrow(positionCorrection, featureId)
        .commandJourneyId();

    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourneyId);

    return getDefineAreaModelAndView(
        correctionId,
        licencePositionCorrectionId,
        featureId,
        correction,
        commandJourneyId,
        activeFeatures
    );
  }

  @PostMapping("/define-area")
  public ModelAndView defineArea(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);

    var commandJourneyId = partialSurrenderCorrectionService
        .getSurrenderDetailsOrThrow(positionCorrection, featureId)
        .commandJourneyId();

    var activeFeatures = commandJourneyService.getActiveFeatures(commandJourneyId);

    if (partialSurrenderDefineAreaValidator.hasErrors(activeFeatures)) {
      return getDefineAreaModelAndView(
          correctionId,
          licencePositionCorrectionId,
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
        positionCorrection,
        featureId,
        activeFeatures.stream().map(Feature::getId).toList()
    );

    return ReverseRouter.redirect(on(PartialSurrenderDefineAreaController.class)
        .renderSelectAreas(correctionId, licencePositionCorrectionId, featureId, null));
  }

  @GetMapping("/select-areas")
  public ModelAndView renderSelectAreas(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var surrenderDetails = partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionCorrection, featureId);
    var activeFeatures = commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyId());

    return getSelectAreasModelAndView(
        correctionId,
        licencePositionCorrectionId,
        featureId,
        correction,
        activeFeatures,
        PartialSurrenderSelectAreasForm.from(surrenderDetails)
    );
  }

  @PostMapping("/select-areas")
  public ModelAndView selectAreas(
      @PathVariable UUID correctionId,
      @PathVariable UUID licencePositionCorrectionId,
      @PathVariable UUID featureId,
      @RequestAttribute("validatedCorrection") LicenceCorrection correction,
      @ModelAttribute("form") PartialSurrenderSelectAreasForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    var positionCorrection = licencePositionCorrectionService
        .getPositionCorrectionForCorrection(licencePositionCorrectionId, correction);
    var surrenderDetails = partialSurrenderCorrectionService.getSurrenderDetailsOrThrow(positionCorrection, featureId);
    var activeFeatures = commandJourneyService.getActiveFeatures(surrenderDetails.commandJourneyId());

    if (partialSurrenderSelectAreasFormValidator.hasErrors(form, bindingResult, activeFeatures)) {
      return getSelectAreasModelAndView(
          correctionId,
          licencePositionCorrectionId,
          featureId,
          correction,
          activeFeatures,
          form
      );
    }

    partialSurrenderCorrectionService.setSurrenderedFeatureIds(
        positionCorrection,
        featureId,
        form.getSurrenderedFeatureIds()
    );

    NotificationBanner.newSuccessBannerWithHeader("Areas to surrender saved", redirectAttributes);

    //TODO - EPGF-183: redirect to ended subareas when implemented
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null)
    );
  }

  private ModelAndView getDefineAreaModelAndView(
      UUID correctionId,
      UUID licencePositionCorrectionId,
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
        .addObject("backLinkUrl", ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(correctionId, licencePositionCorrectionId, featureId, null)));
  }

  private ModelAndView getSelectAreasModelAndView(
      UUID correctionId,
      UUID licencePositionCorrectionId,
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
            .renderDefineArea(correctionId, licencePositionCorrectionId, featureId, null)));
  }
}
