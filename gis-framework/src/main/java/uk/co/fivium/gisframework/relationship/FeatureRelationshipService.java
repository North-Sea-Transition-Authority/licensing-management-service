package uk.co.fivium.gisframework.relationship;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class FeatureRelationshipService {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;

  FeatureRelationshipService(
      FeatureService featureService,
      PolygonService polygonService,
      GrpcClientService grpcClientService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
  }

  /**
   * Checks whether two features overlap in two dimensions, ignoring the depth of their polygons. Each feature's
   * polygons are unioned before the check. Features overlap when they share any area, including when one lies inside
   * the other or they are identical; features that only touch along an edge or at a corner do not overlap.
   *
   * @param feature      The feature to check.
   * @param otherFeature The feature to check against.
   * @return true if the features overlap, false otherwise.
   * @throws IllegalArgumentException if the features are in different coordinate systems, or the feature has no
   *                                  polygons.
   */
  public boolean is2dOverlapping(Feature feature, Feature otherFeature) {
    return !get2dOverlappingFeatures(feature, List.of(otherFeature)).isEmpty();
  }

  /**
   * Finds the features that overlap a feature in two dimensions, ignoring the depth of their polygons. Each feature's
   * polygons are unioned before the check. Features overlap when they share any area, including when one lies inside
   * the other or they are identical; features that only touch along an edge or at a corner do not overlap.
   *
   * @param feature       The feature to check against.
   * @param otherFeatures The features to check.
   * @return The features from otherFeatures that overlap the feature, in the order they were given.
   * @throws IllegalArgumentException if any feature is not in the feature's coordinate system, or the feature has no
   *                                  polygons.
   */
  public List<Feature> get2dOverlappingFeatures(Feature feature, Collection<Feature> otherFeatures) {
    if (otherFeatures.isEmpty()) {
      return List.of();
    }

    var coordinateSystem = feature.getCoordinateSystem();
    if (otherFeatures.stream().anyMatch(otherFeature -> !coordinateSystem.equals(otherFeature.getCoordinateSystem()))) {
      throw new IllegalArgumentException(
          "All features to check for overlap must use the feature's coordinate system %s".formatted(coordinateSystem));
    }

    var esriJsonPolygons = polygonService.getPolygonsAsEsriJson(feature);
    if (esriJsonPolygons.isEmpty()) {
      throw new IllegalArgumentException("Feature %s has no polygons".formatted(feature.getId()));
    }

    var featureIdToPolygonIdToEsriJson =
        polygonService.getFeatureIdToPolygonIdToEsriJson(featureService.getEntityBackedFeatures(otherFeatures));

    var overlappingFeatureIds =
        grpcClientService.find2dOverlappingFeatures(esriJsonPolygons, featureIdToPolygonIdToEsriJson);

    return otherFeatures.stream()
        .filter(otherFeature -> overlappingFeatureIds.contains(otherFeature.getId()))
        .toList();
  }
}
