package uk.co.fivium.gisframework.feature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@RestController
@RequestMapping("/api/gis-framework")
class FeatureRestController {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final ObjectMapper objectMapper;
  private final LineService lineService;
  private final TextualDescriptionService textualDescriptionService;

  FeatureRestController(
      FeatureService featureService,
      PolygonService polygonService,
      ObjectMapper objectMapper,
      LineService lineService,
      TextualDescriptionService textualDescriptionService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.objectMapper = objectMapper;
    this.lineService = lineService;
    this.textualDescriptionService = textualDescriptionService;
  }

  @GetMapping("/features")
  ResponseEntity<JsonFeatures> getFeaturesEsriJson(@RequestParam("featureIds") List<UUID> featureIds)
      throws JsonProcessingException {
    List<Feature> features = featureService.getFeaturesByIds(featureIds);
    List<JsonFeature> esriJsonFeatures = new ArrayList<>();

    for (var feature : features) {
      var esriJsonPolygons = polygonService.getPolygonsAsEsriJson(feature, true);

      var attributes = JsonFeature.Attributes.from(feature);
      for (var esriJsonPolygon : esriJsonPolygons) {
        Map<String, Object> geometry = objectMapper.readValue(esriJsonPolygon, new TypeReference<>() {
        });
        esriJsonFeatures.add(new JsonFeature(geometry, attributes));
      }
    }

    return ResponseEntity.ok(new JsonFeatures(
        esriJsonFeatures,
        JsonFeatures.SpatialReference.from(CoordinateSystem.WGS84)
    ));
  }

  @GetMapping("/outline-nodes")
  ResponseEntity<JsonFeatureOutlineNodesResponse> getOutlineNodes(@RequestParam("featureIds") List<UUID> featureIds) {
    var features = featureService.getFeaturesByIds(featureIds);
    return ResponseEntity.ok(new JsonFeatureOutlineNodesResponse(lineService.getOutlineNodes(features)));
  }

  @GetMapping("/textual-description")
  ResponseEntity<JsonTextualDescription> getTextualDescription(
      @RequestParam("featureId") List<UUID> featureIds
  ) {
    var features = featureService.getFeaturesByIds(featureIds);
    return ResponseEntity.ok(
        new JsonTextualDescription(textualDescriptionService.getTextualDescription(features)));
  }
}
