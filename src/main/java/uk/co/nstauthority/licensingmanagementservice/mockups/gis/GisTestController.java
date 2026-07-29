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
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Controller
@RequestMapping("/mockups/gis-test")
@Profile("mockups")
public class GisTestController {

  private final FeatureService featureService;

  public GisTestController(FeatureService featureService) {
    this.featureService = featureService;
  }

  @GetMapping("/point-and-click")
  public ModelAndView renderSplitByPointAndClick() {
    var ed50Feature = featureService.findFeatureOrThrow(CoordinateSystem.ED50);
    var bngFeature = featureService.findFeatureOrThrow(CoordinateSystem.BRITISH_NATIONAL_GRID);
    return new ModelAndView("lms/mockups/gis/pointAndClickMapTester")
        .addObject("featureIdsEd50", List.of(ed50Feature.getId().toString()))
        .addObject("featureIdsBng", List.of(bngFeature.getId().toString()));
  }

  @GetMapping("/map-with-textual-description/{featureId}")
  public ModelAndView renderMapWithTextualDescription(@PathVariable("featureId") UUID featureId) {
    var feature = featureService.getFeatureOrThrow(featureId);
    return new ModelAndView("lms/mockups/gis/mapWithTextualDescriptionTester")
        .addObject("featureIds", List.of(feature.getId().toString()))
        .addObject("srsWkid", CoordinateSystemUtils.getWkid(feature.getCoordinateSystem()));
  }

  @GetMapping("/split-by-coordinate/{coordinateSystem}")
  public ModelAndView renderSplitByCoordinateEntry(
      @PathVariable("coordinateSystem") CoordinateSystem coordinateSystem,
      @RequestParam(name = "precision", defaultValue = "4") int precision) {
    var feature = featureService.findFeatureOrThrow(coordinateSystem);
    return new ModelAndView("lms/mockups/gis/splitByCoordinateEntryTester")
        .addObject("featureIds", List.of(feature.getId().toString()))
        .addObject("srsWkid", CoordinateSystemUtils.getWkid(coordinateSystem))
        .addObject("precision", precision);
  }
}
