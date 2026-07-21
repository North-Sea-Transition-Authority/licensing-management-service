package uk.co.nstauthority.licensingmanagementservice.mockups.gis;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
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
}
