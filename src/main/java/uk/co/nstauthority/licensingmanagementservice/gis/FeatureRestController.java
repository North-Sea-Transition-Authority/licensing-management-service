package uk.co.nstauthority.licensingmanagementservice.gis;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.JsonFeatureOutlineNodesResponse;
import uk.co.fivium.gisframework.feature.JsonFeatures;
import uk.co.fivium.gisframework.feature.JsonTextualDescription;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.TextualDescriptionService;

@RestController
@RequestMapping("/api/gis-framework")
class FeatureRestController {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;
  private final TextualDescriptionService textualDescriptionService;
  private final CommandJourneyService commandJourneyService;

  FeatureRestController(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      TextualDescriptionService textualDescriptionService,
      CommandJourneyService commandJourneyService) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.textualDescriptionService = textualDescriptionService;
    this.commandJourneyService = commandJourneyService;
  }

  @GetMapping("/features")
  ResponseEntity<JsonFeatures> getFeaturesEsriJson(@RequestParam("featureIds") List<UUID> featureIds) {
    var features = featureService.getFeaturesByIds(featureIds);
    return ResponseEntity.ok(polygonService.getFeaturesAsWgs84EsriJson(features));
  }

  @GetMapping("/command-journey-features/{commandJourneyId}")
  ResponseEntity<JsonFeatures> getCommandJourneyActiveFeatures(@PathVariable UUID commandJourneyId) {
    var features = commandJourneyService.getActiveFeatures(commandJourneyId);
    return ResponseEntity.ok(polygonService.getFeaturesAsWgs84EsriJson(features));
  }

  @GetMapping("/command-journey-outline-nodes/{commandJourneyId}")
  ResponseEntity<JsonFeatureOutlineNodesResponse> getCommandJourneyActiveOutlineNodes(@PathVariable UUID commandJourneyId) {
    var features = commandJourneyService.getActiveFeatures(commandJourneyId);
    return ResponseEntity.ok(new JsonFeatureOutlineNodesResponse(lineService.getOutlineNodes(features)));
  }

  @GetMapping("/outline-nodes")
  ResponseEntity<JsonFeatureOutlineNodesResponse> getOutlineNodes(@RequestParam("featureIds") List<UUID> featureIds) {
    var features = featureService.getFeaturesByIds(featureIds);
    return ResponseEntity.ok(new JsonFeatureOutlineNodesResponse(lineService.getOutlineNodes(features)));
  }

  @GetMapping("/textual-description")
  ResponseEntity<JsonTextualDescription> getTextualDescription(@RequestParam("featureId") List<UUID> featureIds) {
    var features = featureService.getFeaturesByIds(featureIds);
    return ResponseEntity.ok(
        new JsonTextualDescription(textualDescriptionService.getTextualDescription(features)));
  }

  @GetMapping("/command-journey-textual-description/{commandJourneyId}")
  ResponseEntity<JsonTextualDescription> getCommandJourneyTextualDescription(@PathVariable UUID commandJourneyId) {
    var features = commandJourneyService.getActiveFeatures(commandJourneyId);
    return ResponseEntity.ok(
        new JsonTextualDescription(textualDescriptionService.getTextualDescription(features))
    );
  }
}
