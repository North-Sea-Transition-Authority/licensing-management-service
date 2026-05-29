package uk.co.fivium.gisframework.feature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@RestController
@RequestMapping("/api/gis-framework")
class FeatureRestController {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final ObjectMapper objectMapper;

  FeatureRestController(FeatureService featureService,
                        PolygonService polygonService,
                        ObjectMapper objectMapper) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/feature/{featureId}")
  JsonFeatures getFeaturesEsriJson(@PathVariable UUID featureId) throws JsonProcessingException {
    Feature feature;
    try {
      feature = featureService.getFeatureOrThrow(featureId);
    } catch (EntityNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature %s not found".formatted(featureId), e);
    }
    var esriJsonPolygons = polygonService.getPolygonsAsEsriJson(feature, true);

    var attributes = JsonFeature.Attributes.from(feature);
    List<JsonFeature> esriJsonFeatures = new ArrayList<>();
    for (var esriJsonPolygon : esriJsonPolygons) {
      Map<String, Object> geometry = objectMapper.readValue(esriJsonPolygon, new TypeReference<>() {});
      esriJsonFeatures.add(new JsonFeature(geometry, attributes));
    }

    return new JsonFeatures(
        esriJsonFeatures,
        JsonFeatures.SpatialReference.from(CoordinateSystem.WGS84)
    );
  }
}
