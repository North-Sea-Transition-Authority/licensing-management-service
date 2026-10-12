package uk.co.fivium.gisframework.feature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Service
public class PolygonService {

  private final PolygonRepository polygonRepository;
  private final FeatureService featureService;
  private final GrpcClientService grpcClientService;
  private final ObjectMapper objectMapper;

  public PolygonService(PolygonRepository polygonRepository,
                        FeatureService featureService,
                        GrpcClientService grpcClientService,
                        ObjectMapper objectMapper) {
    this.polygonRepository = polygonRepository;
    this.featureService = featureService;
    this.grpcClientService = grpcClientService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void savePolygon(Polygon polygon) {
    polygonRepository.save(polygon);
  }

  @Transactional
  public void savePolygons(Collection<Polygon> polygons) {
    polygonRepository.saveAll(polygons);
  }

  public List<Polygon> findAllByFeature(Feature feature) {
    return polygonRepository.findAllByFeature(feature);
  }

  public List<Polygon> getPolygons(List<Feature> inputFeatures) {
    return polygonRepository.findAllByFeatureIn(inputFeatures);
  }

  /**
   * Generates all the EsriJSON polygons for a given feature.
   *
   * @param feature the feature whose polygons will be built as EsriJSON.
   * @return a list of EsriJSON polygons for the given feature
   */
  public List<String> getPolygonsAsEsriJson(Feature feature) {
    return getPolygonsAsEsriJson(feature, false);
  }

  public List<String> getPolygonsAsEsriJson(Feature feature, boolean projectToWgs84) {
    var entityBackedFeature = featureService.getEntityBackedFeature(feature);
    return getPolygonsAsEsriJson(entityBackedFeature, projectToWgs84);
  }

  /**
   * Builds the WGS84 EsriJSON representation of the given features, ready to return from a REST endpoint.
   *
   * @param features the features to convert.
   * @return the features as EsriJSON, alongside the WGS84 spatial reference.
   */
  public JsonFeatures getFeaturesAsWgs84EsriJson(List<Feature> features) {
    List<JsonFeature> esriJsonFeatures = new ArrayList<>();

    for (var feature : features) {
      var entityBackedFeature = featureService.getEntityBackedFeature(feature);
      for (var polygonToLines : entityBackedFeature.polygonToLines().entrySet()) {
        var esriJsonPolygon = buildPolygonEsriJson(polygonToLines.getValue(), feature.getCoordinateSystem(), true);
        esriJsonFeatures.add(new JsonFeature(
            readGeometry(esriJsonPolygon),
            JsonFeature.Attributes.from(feature, polygonToLines.getKey())));
      }
    }

    return new JsonFeatures(esriJsonFeatures, JsonFeatures.SpatialReference.from(CoordinateSystem.WGS84));
  }

  private Map<String, Object> readGeometry(String esriJsonPolygon) {
    try {
      return objectMapper.readValue(esriJsonPolygon, new TypeReference<>() {
      });
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse generated EsriJSON polygon '%s'".formatted(esriJsonPolygon), e);
    }
  }

  @Transactional
  public void deleteAll() {
    polygonRepository.deleteAll();
  }

  /**
   * Generates the EsriJSON polygon for each of a feature's polygons.
   *
   * @param entityBackedFeature the feature, along with its polygons and lines, whose polygons will be built as EsriJSON.
   * @return the EsriJSON polygons, keyed by polygon id.
   */
  public Map<UUID, String> getPolygonIdToEsriJson(EntityBackedFeature entityBackedFeature) {
    Map<UUID, String> polygonIdToEsriJson = new HashMap<>();

    for (var polygonToLines : entityBackedFeature.polygonToLines().entrySet()) {
      polygonIdToEsriJson.put(
          polygonToLines.getKey().getId(),
          buildPolygonEsriJson(polygonToLines.getValue(), entityBackedFeature.feature().getCoordinateSystem(), false)
      );
    }

    return polygonIdToEsriJson;
  }

  /**
   * Generates the EsriJSON polygon for each polygon of many features in a single call.
   *
   * @param entityBackedFeatures the features, along with their polygons and lines, whose polygons will be built as
   *                             EsriJSON.
   * @return the EsriJSON polygons, keyed by feature id and then polygon id.
   */
  public Map<UUID, Map<UUID, String>> getFeatureIdToPolygonIdToEsriJson(
      Collection<EntityBackedFeature> entityBackedFeatures
  ) {
    return grpcClientService.buildFeaturePolygons(entityBackedFeatures);
  }

  private List<String> getPolygonsAsEsriJson(
      EntityBackedFeature entityBackedFeature,
      boolean projectToWgs84
  ) {
    return entityBackedFeature.polygonToLines()
        .values()
        .stream()
        .map(lines -> buildPolygonEsriJson(lines, entityBackedFeature.feature().getCoordinateSystem(), projectToWgs84))
        .toList();
  }

  private String buildPolygonEsriJson(
      List<Line> lines,
      CoordinateSystem coordinateSystem,
      boolean projectToWgs84
  ) {
    return grpcClientService.buildPolygon(
        LineUtils.getEsriJsonsInDisplayOrder(lines),
        coordinateSystem,
        projectToWgs84
    );
  }
}
