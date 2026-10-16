package uk.co.fivium.gisframework.operator;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polyline;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class MergeOperatorService {

  private final GrpcClientService grpcClientService;
  private final OperatorResultProcessingService operatorResultProcessingService;
  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;
  private final MergeService mergeService;

  public MergeOperatorService(
      GrpcClientService grpcClientService,
      OperatorResultProcessingService operatorResultProcessingService,
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      MergeService mergeService
  ) {
    this.grpcClientService = grpcClientService;
    this.operatorResultProcessingService = operatorResultProcessingService;
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.mergeService = mergeService;
  }

  @Transactional
  public Feature mergePolygons(List<Feature> inputFeatures) {
    if (inputFeatures.size() < 2) {
      throw new IllegalArgumentException("Must provide at least two input features");
    }

    var polygonsWithSources = toPolygonsWithSources(inputFeatures);

    var mergedPolygonsWithSources = anyHaveDepth(polygonsWithSources)
        ? mergeService.mergeWithDepths(polygonsWithSources)
        : mergeService.mergeIn2d(polygonsWithSources);

    var newFeature = operatorResultProcessingService.processOutputPolygons(
        inputFeatures,
        mergedPolygonsWithSources
    );

    removeInnerVertices(
        mergedPolygonsWithSources.stream().map(EsriJsonPolygonWithSources::esriJson).toList(),
        newFeature
    );

    return newFeature;
  }

  private List<EsriJsonPolygonWithSources> toPolygonsWithSources(List<Feature> features) {
    List<EsriJsonPolygonWithSources> polygonsWithSources = new ArrayList<>();
    for (var feature : features) {
      var entityBackedFeature = featureService.getEntityBackedFeature(feature);
      var polygonIdToEsriJson = polygonService.getPolygonIdToEsriJson(entityBackedFeature);
      for (var polygon : entityBackedFeature.polygonToLines().keySet()) {
        polygonsWithSources.add(
            new EsriJsonPolygonWithSources(polygonIdToEsriJson.get(polygon.getId()), List.of(polygon))
        );
      }
    }
    return polygonsWithSources;
  }

  private boolean anyHaveDepth(List<EsriJsonPolygonWithSources> polygonsWithSources) {
    return polygonsWithSources.stream()
        .flatMap(polygonWithSources -> polygonWithSources.sources().stream())
        .anyMatch(polygon -> polygon.getStartDepth() != null || polygon.getEndDepth() != null);
  }

  /**
   * After a merge, the output polygon may contain collinear vertices that were boundaries
   * between input lines from different parents. If two adjacent lines at such a vertex share
   * the same attributes, merge them into a single line entity (removing the redundant vertex).
   * If they have different attributes, keep them separate so each retains
   * its parent's attributes.
   *
   * <p>Each polygon's inner vertices are only removed from its own rings, as a polygon at another depth can have a
   * real corner at the same point.
   */
  private void removeInnerVertices(List<String> polygons, Feature newFeature) {
    var entityBackedFeature = featureService.getEntityBackedFeature(newFeature);
    var polygonIdToEsriJson = polygonService.getPolygonIdToEsriJson(entityBackedFeature);

    List<Line> linesToDelete = new ArrayList<>();
    for (var polygonToLines : entityBackedFeature.polygonToLines().entrySet()) {
      var innerVertices = findInnerVertices(polygonIdToEsriJson.get(polygonToLines.getKey().getId()));
      if (!innerVertices.isEmpty()) {
        for (var ringLines : getRingsSorted(polygonToLines.getValue())) {
          linesToDelete.addAll(mergeLinesAtInnerVertices(ringLines, innerVertices));
        }
      }
    }

    if (!linesToDelete.isEmpty()) {
      lineService.deleteLines(linesToDelete);

      var remainingLines = createRemainingLines(entityBackedFeature, linesToDelete);

      operatorResultProcessingService.numberLines(remainingLines);
      operatorResultProcessingService.validateLinesAreValid(remainingLines, polygons);
      lineService.saveLines(remainingLines);
    }
  }

  /**
   * Merges consecutive lines in the ring that meet at an inner vertex and share the same attributes.
   *
   * @return the lines merged into their neighbour, to delete.
   */
  private List<Line> mergeLinesAtInnerVertices(List<Line> ringLines, Set<Point2D> innerVertices) {
    List<Line> linesToDelete = new ArrayList<>();
    int i = 0;
    while (i < ringLines.size()) {
      Line current = ringLines.get(i);
      Line next;

      if (i == ringLines.size() - 1) {
        //Check last line to first line in the ring
        next = ringLines.getFirst();
      } else {
        next = ringLines.get(i + 1);
      }

      Point2D currentEndPoint = getPolylineEndPoint(current.getEsriJson());

      if (innerVertices.contains(currentEndPoint) && Objects.equals(current.getAttributes(), next.getAttributes())) {
        String combinedLineEsriJson = grpcClientService.mergeAndGeneralizeLines(
            List.of(current.getEsriJson(), next.getEsriJson())
        );

        current.setEsriJson(combinedLineEsriJson);
        linesToDelete.add(next);
        ringLines.remove(next);
      } else {
        i++;
      }
    }
    return linesToDelete;
  }

  private List<List<Line>> getRingsSorted(List<Line> polygonLines) {
    List<List<Line>> rings = new ArrayList<>();
    for (var ringLines : polygonLines.stream().collect(Collectors.groupingBy(Line::getRingNumber)).values()) {
      ringLines.sort(Comparator.comparing(Line::getDisplayOrder));
      rings.add(ringLines);
    }
    return rings;
  }

  private List<Line> createRemainingLines(EntityBackedFeature entityBackedFeature, List<Line> linesToDelete) {
    List<Line> remainingLines = new ArrayList<>();
    for (var polygonLines : entityBackedFeature.polygonToLines().values()) {
      for (var line : polygonLines) {
        if (!linesToDelete.contains(line)) {
          remainingLines.add(line);
        }
      }
    }
    return remainingLines;
  }

  /**
   * Find vertices that exist in the original polygon but were removed by generalise.
   * These are collinear (inner) vertices that don't contribute to the polygon shape.
   */
  private Set<Point2D> findInnerVertices(String originalJson) {
    String generalizedPolygon = grpcClientService.generalizePolygon(originalJson);

    Set<Point2D> originalVertices = getPolygonVertices(originalJson);
    Set<Point2D> generalizedVertices = getPolygonVertices(generalizedPolygon);
    originalVertices.removeAll(generalizedVertices);
    return originalVertices;
  }

  private Set<Point2D> getPolygonVertices(String esriJsonPolygon) {
    MapGeometry mapGeometry = OperatorImportFromJson.local()
        .execute(Geometry.Type.Polygon, esriJsonPolygon);
    var esriPolygon = (com.esri.core.geometry.Polygon) mapGeometry.getGeometry();

    Set<Point2D> vertices = new HashSet<>();
    for (int i = 0; i < esriPolygon.getPointCount(); i++) {
      vertices.add(esriPolygon.getPoint(i).getXY());
    }
    return vertices;
  }

  private Point2D getPolylineEndPoint(String esriJsonPolyline) {
    MapGeometry mapGeometry = OperatorImportFromJson.local()
        .execute(Geometry.Type.Polyline, esriJsonPolyline);
    Polyline polyline = (Polyline) mapGeometry.getGeometry();
    int endIndex = polyline.getPathEnd(0) - 1;
    return polyline.getPoint(endIndex).getXY();
  }
}
