package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionLicenceIsType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrection;
import uk.co.nstauthority.licensingmanagementservice.fds.error.ErrorSummaryItem;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.PartialSurrenderCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.tasklist.PartialSurrenderTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Controller
@RequestMapping(
    "/licence-corrections/{correctionId}/position-correction" +
        "/{licencePositionCorrectionId}/partial-surrender/{featureId}"
)
@InvokingUserCanViewCorrection
@CorrectionLicenceIsType({LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION})
public class PartialSurrenderDefineAreaController {

  private static final String PAGE_TITLE = "Define area to surrender";
  private static final String NO_SPLIT_ERROR = "You must split the block before continuing";

  private final CommandJourneyService commandJourneyService;
  private final PartialSurrenderCorrectionService partialSurrenderCorrectionService;
  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator;

  public PartialSurrenderDefineAreaController(
      CommandJourneyService commandJourneyService,
      PartialSurrenderCorrectionService partialSurrenderCorrectionService,
      LicencePositionCorrectionService licencePositionCorrectionService,
      PartialSurrenderDefineAreaValidator partialSurrenderDefineAreaValidator
  ) {
    this.commandJourneyService = commandJourneyService;
    this.partialSurrenderCorrectionService = partialSurrenderCorrectionService;
    this.licencePositionCorrectionService = licencePositionCorrectionService;
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

    //TODO - EPGF-182: Select the areas to surrender
    return ReverseRouter.redirect(on(PartialSurrenderTaskListController.class)
        .renderTaskList(correctionId, licencePositionCorrectionId, null, null));
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
        .addObject("pageTitle", PAGE_TITLE)
        .addObject("backLinkUrl", ReverseRouter.route(on(BlockSurrenderTypeController.class)
            .renderSurrenderTypeForm(correctionId, licencePositionCorrectionId, featureId, null)));
  }
}
