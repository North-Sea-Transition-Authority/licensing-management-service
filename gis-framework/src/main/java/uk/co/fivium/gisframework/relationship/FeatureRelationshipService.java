package uk.co.fivium.gisframework.relationship;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
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

    return getFeaturesOverlappingPolygons(esriJsonPolygons, otherFeatures);
  }

  /**
   * Finds the features that overlap the union of several features in two dimensions, ignoring the depth of their
   * polygons. A feature overlaps the union when it shares any area with at least one of the features; features that
   * only touch along an edge or at a corner do not overlap.
   *
   * @param features      The features whose union to check against.
   * @param otherFeatures The features to check.
   * @return The features from otherFeatures that overlap the union of the features, in the order they were given.
   * @throws IllegalArgumentException if no features are given, the features and other features are not all in the
   *                                  same coordinate system, or the features have no polygons.
   */
  public List<Feature> get2dOverlappingFeatures(
      Collection<Feature> features,
      Collection<Feature> otherFeatures
  ) {
    if (features.isEmpty()) {
      throw new IllegalArgumentException("Must provide at least one feature to check for overlap");
    }

    if (otherFeatures.isEmpty()) {
      return List.of();
    }

    var coordinateSystem = features.iterator().next().getCoordinateSystem();
    if (Stream.concat(features.stream(), otherFeatures.stream())
        .anyMatch(feature -> !coordinateSystem.equals(feature.getCoordinateSystem()))) {
      throw new IllegalArgumentException(
          "All features to check for overlap must use the coordinate system %s".formatted(coordinateSystem)
      );
    }

    var esriJsonPolygons = featureService.getEntityBackedFeatures(features).stream()
        .flatMap(entityBackedFeature -> polygonService.getPolygonIdToEsriJson(entityBackedFeature).values().stream())
        .toList();
    if (esriJsonPolygons.isEmpty()) {
      throw new IllegalArgumentException("Features to check for overlap have no polygons");
    }

    return getFeaturesOverlappingPolygons(esriJsonPolygons, otherFeatures);
  }

  private List<Feature> getFeaturesOverlappingPolygons(
      List<String> esriJsonPolygons,
      Collection<Feature> otherFeatures
  ) {
    var featureIdToPolygonIdToEsriJson =
        polygonService.getFeatureIdToPolygonIdToEsriJson(featureService.getEntityBackedFeatures(otherFeatures));

    var overlappingFeatureIds =
        grpcClientService.find2dOverlappingFeatures(esriJsonPolygons, featureIdToPolygonIdToEsriJson);

    return otherFeatures.stream()
        .filter(otherFeature -> overlappingFeatureIds.contains(otherFeature.getId()))
        .toList();
  }

  /**
   * Checks whether the union of the features covers exactly the same shape as another feature in two dimensions,
   * ignoring the depth of their polygons.
   *
   * @param features     The features to union and compare.
   * @param otherFeature The feature to compare against.
   * @return true if the unioned features are topologically equal to the other feature, false otherwise.
   * @throws IllegalArgumentException if no features are given, or any feature is not in the other feature's coordinate
   *                                  system.
   */
  public boolean is2dTopologicallyEqual(Collection<Feature> features, Feature otherFeature) {
    if (features.isEmpty()) {
      throw new IllegalArgumentException("Must provide at least one feature to compare");
    }

    var coordinateSystem = otherFeature.getCoordinateSystem();
    if (features.stream().anyMatch(feature -> !coordinateSystem.equals(feature.getCoordinateSystem()))) {
      throw new IllegalArgumentException(
          "All features to compare must use the other feature's coordinate system %s".formatted(coordinateSystem));
    }

    var polygonLines = featureService.getEntityBackedFeatures(features).stream()
        .flatMap(entityBackedFeature -> entityBackedFeature.polygonToLines().values().stream())
        .map(lines -> lines.stream().map(Line::getEsriJson).toList())
        .toList();

    return grpcClientService
        .validateTopologicallyEqual(polygonLines, featureService.getEntityBackedFeature(otherFeature))
        .getIsValid();
  }
}
