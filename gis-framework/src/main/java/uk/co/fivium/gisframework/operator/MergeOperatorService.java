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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@Service
public class MergeOperatorService {

  private final GrpcClientService grpcClientService;
  private final OperatorResultProcessingService operatorResultProcessingService;
  private final PolygonService polygonService;
  private final LineService lineService;

  public MergeOperatorService(
      GrpcClientService grpcClientService,
      OperatorResultProcessingService operatorResultProcessingService,
      PolygonService polygonService,
      LineService lineService
  ) {
    this.grpcClientService = grpcClientService;
    this.operatorResultProcessingService = operatorResultProcessingService;
    this.polygonService = polygonService;
    this.lineService = lineService;
  }

  @Transactional
  public Feature mergePolygons(List<Feature> inputFeatures) {
    if (inputFeatures.size() < 2) {
      throw new IllegalArgumentException("Must provide at least two input features");
    }

    String resultEsriJsonPolygon = inputFeatures.stream()
        .flatMap(feature -> polygonService.getPolygonsAsEsriJson(feature, false).stream())
        .reduce(grpcClientService::mergePolygons)
        .orElseThrow();

    List<String> singlePartPolygons = grpcClientService.multiPartToSinglePart(resultEsriJsonPolygon);

    var newFeature = operatorResultProcessingService.processOutputPolygons(inputFeatures, singlePartPolygons, 0);

    removeInnerVertices(singlePartPolygons, newFeature);

    return newFeature;
  }

  /**
   * After a merge, the output polygon may contain collinear vertices that were boundaries
   * between input lines from different parents. If two adjacent lines at such a vertex share
   * the same attributes, merge them into a single line entity (removing the redundant vertex).
   * If they have different attributes, keep them separate so each retains
   * its parent's attributes.
   */
  private void removeInnerVertices(List<String> polygons, Feature newFeature) {
    Set<Point2D> innerVertices = polygons.stream()
        .flatMap(polygon -> findInnerVertices(polygon).stream())
        .collect(Collectors.toCollection(HashSet::new));

    if (innerVertices.isEmpty()) {
      return;
    }

    List<List<Line>> newFeatureLines = getFeatureLinesByRingSorted(newFeature);

    List<Line> linesToDelete = new ArrayList<>();
    for (List<Line> ringLines : newFeatureLines) {
      // Merge consecutive lines that share the same attributes at inner vertices
      int i = 0;
      while (i < ringLines.size()) {

        Line current = ringLines.get(i);
        Line next;

        if (i ==  ringLines.size() - 1) {
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
    }

    if (!linesToDelete.isEmpty()) {
      lineService.deleteLines(linesToDelete);

      List<Line> remainingLines = newFeatureLines.stream()
          .flatMap(List::stream)
          .toList();

      operatorResultProcessingService.numberLines(remainingLines);
      operatorResultProcessingService.validateLinesAreValid(remainingLines, polygons);
      lineService.saveLines(remainingLines);
    }
  }

  private List<List<Line>> getFeatureLinesByRingSorted(Feature newFeature) {
    var featurePolygons = polygonService.findAllByFeature(newFeature);
    var allLines = lineService.getLines(featurePolygons);

    // Group by polygon to avoid mixing rings from different polygons
    Map<Polygon, Map<Integer, List<Line>>> polygonToRingLines = allLines.stream()
        .collect(Collectors.groupingBy(
            Line::getPolygon,
            Collectors.groupingBy(Line::getRingNumber)
        ));

    List<List<Line>> result = new ArrayList<>();
    for (var ringToLines : polygonToRingLines.values()) {
      for (List<Line> ringLines : ringToLines.values()) {
        ringLines.sort(Comparator.comparing(Line::getDisplayOrder));
        result.add(ringLines);
      }
    }
    return result;
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
