package uk.co.fivium.gisframework.relationship;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.Polygon;
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
    validateFeaturesToCompare(features, otherFeature);

    var polygonsLines = featureService.getEntityBackedFeatures(features).stream()
        .flatMap(entityBackedFeature -> entityBackedFeature.polygonToLines().values().stream())
        .toList();

    return is2dTopologicallyEqual(polygonsLines, featureService.getEntityBackedFeature(otherFeature));
  }

  /**
   * Checks whether the union of the features covers exactly the same shape as another feature in three dimensions.
   * Both sides are swept down through every depth where either side's polygons start or end, and at each depth the
   * polygons covering it are unioned and compared in two dimensions. A depth covered by only one side is not equal; a
   * depth covered by neither side is skipped.
   *
   * @param features     The features to union and compare.
   * @param otherFeature The feature to compare against.
   * @return true if the unioned features are topologically equal to the other feature at every depth, false otherwise.
   * @throws IllegalArgumentException if no features are given, any feature is not in the other feature's coordinate
   *                                  system, or either side has no polygons.
   */
  public boolean is3dTopologicallyEqual(Collection<Feature> features, Feature otherFeature) {
    validateFeaturesToCompare(features, otherFeature);

    Map<Polygon, List<Line>> polygonToLines = new LinkedHashMap<>();
    featureService.getEntityBackedFeatures(features)
        .forEach(entityBackedFeature -> polygonToLines.putAll(entityBackedFeature.polygonToLines()));

    var otherPolygonToLines = featureService.getEntityBackedFeature(otherFeature).polygonToLines();

    for (var depth : getDepthsShallowestFirst(polygonToLines.keySet(), otherPolygonToLines.keySet())) {
      var polygonToLinesAtDepth = getPolygonsCoveringDepth(polygonToLines, depth);
      var otherPolygonToLinesAtDepth = getPolygonsCoveringDepth(otherPolygonToLines, depth);

      if (polygonToLinesAtDepth.isEmpty() && otherPolygonToLinesAtDepth.isEmpty()) {
        continue;
      }

      if (polygonToLinesAtDepth.isEmpty() || otherPolygonToLinesAtDepth.isEmpty()) {
        return false;
      }

      var is2dEqual = is2dTopologicallyEqual(
          polygonToLinesAtDepth.values(),
          new EntityBackedFeature(otherFeature, otherPolygonToLinesAtDepth)
      );
      if (!is2dEqual) {
        return false;
      }
    }

    return true;
  }

  private boolean is2dTopologicallyEqual(Collection<List<Line>> polygonsLines, EntityBackedFeature otherFeature) {
    return grpcClientService
        .validateTopologicallyEqual(toPolygonLineEsriJsons(polygonsLines), otherFeature)
        .getIsValid();
  }

  private void validateFeaturesToCompare(Collection<Feature> features, Feature otherFeature) {
    if (features.isEmpty()) {
      throw new IllegalArgumentException("Must provide at least one feature to compare");
    }

    var coordinateSystem = otherFeature.getCoordinateSystem();
    if (features.stream().anyMatch(feature -> !coordinateSystem.equals(feature.getCoordinateSystem()))) {
      throw new IllegalArgumentException(
          "All features to compare must use the other feature's coordinate system %s".formatted(coordinateSystem));
    }
  }

  private SortedSet<Long> getDepthsShallowestFirst(Collection<Polygon> polygons, Collection<Polygon> otherPolygons) {
    SortedSet<Long> depths = new TreeSet<>(Comparator.reverseOrder());
    Stream.concat(polygons.stream(), otherPolygons.stream())
        .forEach(polygon -> {
          depths.add(polygon.getEffectiveStartDepth());
          depths.add(polygon.getEffectiveEndDepth());
        });
    return depths;
  }

  private Map<Polygon, List<Line>> getPolygonsCoveringDepth(Map<Polygon, List<Line>> polygonToLines, long depth) {
    return polygonToLines.entrySet().stream()
        .filter(entry -> entry.getKey().coversDepth(depth))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (lines, duplicateLines) -> lines,
            LinkedHashMap::new
        ));
  }

  private List<List<String>> toPolygonLineEsriJsons(Collection<List<Line>> polygonsLines) {
    return polygonsLines.stream()
        .map(lines -> lines.stream().map(Line::getEsriJson).toList())
        .toList();
  }
}
