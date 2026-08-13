package uk.co.nstauthority.licensingmanagementservice.mockups.gis;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.FeatureJourneyStateService;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Controller
@RequestMapping("/mockups/gis-test")
@Profile("mockups")
public class GisTestController {

  public static final String SRS_WKID_MODEL_NAME = "srsWkid";

  private final FeatureService featureService;
  private final CommandJourneyService commandJourneyService;
  private final FeatureJourneyStateService featureJourneyStateService;

  public GisTestController(
      FeatureService featureService,
      CommandJourneyService commandJourneyService,
      FeatureJourneyStateService featureJourneyStateService) {
    this.featureService = featureService;
    this.commandJourneyService = commandJourneyService;
    this.featureJourneyStateService = featureJourneyStateService;
  }

  @GetMapping("/point-and-click/{coordinateSystem}")
  public ModelAndView renderSplitByPointAndClick(@PathVariable("coordinateSystem") CoordinateSystem coordinateSystem) {
    var feature = featureJourneyStateService.findFeatureWithNoJourneyStateOrThrow(coordinateSystem);
    var commandJourney = commandJourneyService.createAndAssignCommandJourney(List.of(feature));
    return new ModelAndView("lms/mockups/gis/pointAndClickMapTester")
        .addObject("commandJourneyId", commandJourney.getId().toString())
        .addObject(SRS_WKID_MODEL_NAME, CoordinateSystemUtils.getWkid(coordinateSystem));
  }

  @GetMapping("/map-with-textual-description/{featureId}")
  public ModelAndView renderMapWithTextualDescription(@PathVariable("featureId") UUID featureId) {
    var feature = featureService.getFeatureOrThrow(featureId);
    return new ModelAndView("lms/mockups/gis/mapWithTextualDescriptionTester")
        .addObject("featureIds", List.of(feature.getId().toString()))
        .addObject(SRS_WKID_MODEL_NAME, CoordinateSystemUtils.getWkid(feature.getCoordinateSystem()));
  }

  @GetMapping("/split-by-coordinate/{coordinateSystem}")
  public ModelAndView renderSplitByCoordinateEntry(
      @PathVariable("coordinateSystem") CoordinateSystem coordinateSystem,
      @RequestParam(name = "precision", defaultValue = "4") int precision) {
    var feature = featureJourneyStateService.findFeatureWithNoJourneyStateOrThrow(coordinateSystem);
    return new ModelAndView("lms/mockups/gis/splitByCoordinateEntryTester")
        .addObject("featureIds", List.of(feature.getId().toString()))
        .addObject(SRS_WKID_MODEL_NAME, CoordinateSystemUtils.getWkid(coordinateSystem))
        .addObject("precision", precision);
  }
}
