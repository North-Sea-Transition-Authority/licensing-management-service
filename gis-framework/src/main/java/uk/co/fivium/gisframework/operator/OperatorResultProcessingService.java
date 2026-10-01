package uk.co.fivium.gisframework.operator;

import com.esri.core.geometry.Point;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.LineNavigationType;

@Service
public class OperatorResultProcessingService {

  private static final Comparator<LineWithStartEndPoints> TOP_MOST_THEN_WEST_MOST =
      Comparator.comparingDouble((LineWithStartEndPoints lineWrapper) -> lineWrapper.start().getY()).reversed()
          .thenComparingDouble(lineWrapper -> lineWrapper.start().getX());

  private final PolygonService polygonService;
  private final LineService lineService;
  private final GrpcClientService grpcClientService;
  private final FeatureService featureService;

  OperatorResultProcessingService(PolygonService polygonService,
                                  LineService lineService,
                                  GrpcClientService grpcClientService,
                                  FeatureService featureService) {
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.grpcClientService = grpcClientService;
    this.featureService = featureService;
  }

  @Transactional
  public Feature processOutputPolygon(
      List<Feature> inputFeatures,
      String outputEsriJsonPolygon,
      int resultFeatureNameSuffix
  ) {
    return processPolygons(inputFeatures, List.of(outputEsriJsonPolygon), resultFeatureNameSuffix);
  }

  @Transactional
  public Feature processOutputPolygons(
      List<Feature> inputFeatures,
      List<String> outputEsriJsonPolygons,
      int resultFeatureNameSuffix
  ) {
    return processPolygons(inputFeatures, outputEsriJsonPolygons, resultFeatureNameSuffix);
  }

  private Feature processPolygons(
      List<Feature> inputFeatures,
      List<String> outputEsriJsonPolygons,
      int resultFeatureNameSuffix
  ) {
    var inputPolygons = polygonService.getPolygons(inputFeatures);
    var inputPolygonLines = lineService.getLines(inputPolygons);

    List<List<Line>> linesPerPolygon = outputEsriJsonPolygons.stream()
        .map(json -> buildLinesWithParentAttributes(json, inputPolygonLines))
        .toList();

    List<Line> allLines = linesPerPolygon.stream().flatMap(List::stream).toList();

    var newFeature = buildFeature(inputFeatures, allLines, resultFeatureNameSuffix);

    List<Polygon> newPolygons = new ArrayList<>();
    for (List<Line> polygonLines : linesPerPolygon) {
      var newPolygon = buildPolygon(inputPolygons, newFeature);
      polygonLines.forEach(line -> line.setPolygon(newPolygon));
      newPolygons.add(newPolygon);
    }

    numberLines(allLines);
    validateLinesAreValid(allLines, outputEsriJsonPolygons);

    featureService.saveFeature(newFeature);
    newPolygons.forEach(polygonService::savePolygon);
    lineService.saveLines(allLines);
    return newFeature;
  }

  /**
   * Creates a new feature from the input feature with some of its polygons cropped or removed. Polygons that are
   * neither cropped nor removed are copied across unchanged. The input feature is left untouched.
   *
   * @param inputFeature               the feature to create the cropped feature from.
   * @param polygonIdToCroppedEsriJson the EsriJSON each cropped polygon is replaced with, keyed by polygon id.
   * @param removedPolygonIds          the ids of the polygons to leave out of the new feature.
   * @return the new cropped feature.
   */
  @Transactional
  public Feature processCroppedFeature(
      EntityBackedFeature inputFeature,
      Map<UUID, String> polygonIdToCroppedEsriJson,
      Set<UUID> removedPolygonIds
  ) {
    var newFeature = new Feature();
    List<Polygon> newPolygons = new ArrayList<>();
    List<Line> newLineEntities = new ArrayList<>();
    List<List<Line>> newPolygonLines = new ArrayList<>();
    Map<UUID, List<Line>> croppedPolygonIdToLines = new HashMap<>();

    for (var polygonToLines : inputFeature.polygonToLines().entrySet()) {
      var inputPolygon = polygonToLines.getKey();
      if (removedPolygonIds.contains(inputPolygon.getId())) {
        continue;
      }

      var croppedEsriJson = polygonIdToCroppedEsriJson.get(inputPolygon.getId());
      var polygonLines = croppedEsriJson == null
          ? copyLines(polygonToLines.getValue())
          : buildLinesWithParentAttributes(croppedEsriJson, polygonToLines.getValue());

      var newPolygon = buildPolygon(List.of(inputPolygon), newFeature);
      polygonLines.forEach(line -> line.setPolygon(newPolygon));

      if (croppedEsriJson != null) {
        croppedPolygonIdToLines.put(inputPolygon.getId(), polygonLines);
      }
      newPolygons.add(newPolygon);
      newPolygonLines.add(polygonLines);
      newLineEntities.addAll(polygonLines);
    }

    numberLines(newLineEntities);
    croppedPolygonIdToLines.forEach((polygonId, lines) ->
        validateLinesAreValid(lines, polygonIdToCroppedEsriJson.get(polygonId)));

    var target = inputFeature.feature();
    //TODO - EPGF-84: Take into account 3D shapes when calculating the area
    var featureArea = newPolygonLines.stream()
        .map(polygonLines -> grpcClientService.calculateArea(target.getCoordinateSystem(), polygonLines))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    copyFeatureProperties(target, newFeature, target.getFeatureName(), featureArea);

    featureService.saveFeature(newFeature);
    polygonService.savePolygons(newPolygons);
    lineService.saveLines(newLineEntities);
    return newFeature;
  }

  private List<Line> copyLines(List<Line> lines) {
    return lines.stream()
        .map(line -> {
          var newLineEntity = new Line();
          newLineEntity.setEsriJson(line.getEsriJson());
          newLineEntity.setAttributes(new HashMap<>(line.getAttributes()));
          newLineEntity.setNavigationType(line.getNavigationType());
          return newLineEntity;
        })
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Creates new line entities for the output polygon, copying attributes from the parent line if a parent line can be
   * found for the output line. If no parent line can be found, a new line entity is created with no attributes.
   *
   * @param outputPolygonEsriJson the raw EsriJSON of the output polygons after a polygon operation.
   * @param inputPolygonLines     the lines of the input polygons of the operation.
   * @return a list of new line entities with attributes copied from the parent line if possible.
   */
  private List<Line> buildLinesWithParentAttributes(String outputPolygonEsriJson,
                                                    List<Line> inputPolygonLines) {
    var explodedPolylinesEsriJson = grpcClientService.explodePolygon(outputPolygonEsriJson);
    var findParentLineResponse = grpcClientService.findParentLines(inputPolygonLines, explodedPolylinesEsriJson);

    Map<UUID, Line> idToParentLine = inputPolygonLines.stream()
        .collect(Collectors.toMap(Line::getId, Function.identity()));
    var isOnshore = inputPolygonLines.stream()
        .anyMatch(line -> LineNavigationType.CARTESIAN.equals(line.getNavigationType()));

    List<Line> newLineEntities = findParentLineResponse.polylineToParentLineId()
        .entrySet()
        .stream()
        .map(polylineToParentId -> {
          Line parentLine = idToParentLine.get(polylineToParentId.getValue());
          var newLineEntity = new Line();
          newLineEntity.setEsriJson(polylineToParentId.getKey());
          newLineEntity.setAttributes(new HashMap<>(parentLine.getAttributes()));
          newLineEntity.setNavigationType(parentLine.getNavigationType());
          return newLineEntity;
        })
        .collect(Collectors.toCollection(ArrayList::new));

    findParentLineResponse.orphanLines().forEach(polyline -> {
      var newLineEntity = new Line();
      newLineEntity.setEsriJson(polyline);
      newLineEntity.setAttributes(new HashMap<>());
      newLineEntity.setNavigationType(isOnshore ? LineNavigationType.CARTESIAN : LineNavigationType.LOXODROME);
      newLineEntities.add(newLineEntity);
    });
    return newLineEntities;
  }

  /**
   * Numbers the lines of the output polygon by finding connected lines and assigning them the same ring number
   * and a connection order based on how they are connected. Lines are grouped by their polygon, and the polygon
   * groups are processed in top most then west most order: within a polygon the outer ring is numbered before its
   * inner rings, and the ring number and connection order continue on across rings and polygons.
   *
   * @param unorderedLines the lines of the output polygon.
   */
  public void numberLines(List<Line> unorderedLines) {
    var allLineWithStartEndPoints = grpcClientService.getLineStartAndEndPoints(unorderedLines, false);

    int ringNumberCounter = 0;
    int ringConnectionOrderCounter = 0;
    for (var polygonLines : groupAndOrderLinesByPolygon(allLineWithStartEndPoints)) {
      var linePool = new ArrayList<>(polygonLines);
      linePool.sort(TOP_MOST_THEN_WEST_MOST);

      while (!linePool.isEmpty()) {
        LineWithStartEndPoints current = linePool.removeFirst();

        ringConnectionOrderCounter++;
        current.line().setRingNumber(ringNumberCounter);
        current.line().setDisplayOrder(ringConnectionOrderCounter);

        var isRingClosed = false;
        while (!isRingClosed && !linePool.isEmpty()) {
          Point targetStart = current.end();
          Optional<LineWithStartEndPoints> nextLine = findNextLine(linePool, targetStart);

          if (nextLine.isPresent()) {
            current = nextLine.get();
            linePool.remove(current);
            ringConnectionOrderCounter++;
            current.line().setRingNumber(ringNumberCounter);
            current.line().setDisplayOrder(ringConnectionOrderCounter);
          } else {
            isRingClosed = true;
          }
        }
        ringNumberCounter++;
      }
    }
  }

  /**
   * Groups the lines by their polygon, then orders the polygon groups by their top most then west most start point
   * so that numbering begins on the top most, west most polygon.
   */
  private List<List<LineWithStartEndPoints>> groupAndOrderLinesByPolygon(
      List<LineWithStartEndPoints> lineWrappers) {
    var linesByPolygon = new HashMap<Polygon, List<LineWithStartEndPoints>>();
    for (var lineWrapper : lineWrappers) {
      linesByPolygon
          .computeIfAbsent(lineWrapper.line().getPolygon(), polygon -> new ArrayList<>())
          .add(lineWrapper);
    }

    return linesByPolygon.values()
        .stream()
        .sorted(Comparator.comparing(
            polygonLines -> polygonLines.stream().min(TOP_MOST_THEN_WEST_MOST).orElseThrow(),
            TOP_MOST_THEN_WEST_MOST))
        .toList();
  }

  private Optional<LineWithStartEndPoints> findNextLine(List<LineWithStartEndPoints> linePool,
                                                        Point targetStart) {
    return linePool.stream()
        .filter(lineWrapper -> lineWrapper.start().getXY().equals(targetStart.getXY()))
        .findFirst();
  }

  public void validateLinesAreValid(List<Line> newLineEntities, String outputPolygonEsriJson) {
    validateLines(newLineEntities, List.of(outputPolygonEsriJson));
  }

  public void validateLinesAreValid(List<Line> newLineEntities, List<String> polygons) {
    validateLines(newLineEntities, polygons);
  }

  private void validateLines(List<Line> newLineEntities, List<String> polygons) {
    Map<Polygon, List<Line>> remainingByPolygon = newLineEntities.stream()
        .collect(Collectors.groupingBy(Line::getPolygon));

    List<String> unmatchedParts = new ArrayList<>(polygons);
    for (var polyLines : remainingByPolygon.values()) {
      String matched = unmatchedParts.stream()
          .filter(polygon -> grpcClientService.validatePolygonReconstructionFromPolylines(polyLines, polygon))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(
              "Cannot generate valid polygon from processed lines with EsriJSON: %s"
                  .formatted(polyLines.stream().map(Line::getEsriJson).toList())));

      unmatchedParts.remove(matched);
    }
  }

  private Feature buildFeature(List<Feature> inputFeatures,
                               List<Line> newLineEntities,
                               int featureNameSuffix) {
    var newFeature = new Feature();
    var target = inputFeatures.getFirst();

    String featureName;
    if (inputFeatures.size() == 1) {
      featureName = "%s_%s".formatted(target.getFeatureName(), featureNameSuffix);
    } else {
      //merge operation
      featureName = "mergeResult_%s".formatted(UUID.randomUUID().toString().substring(0, 4).toUpperCase());
    }

    var featureArea = grpcClientService.calculateArea(target.getCoordinateSystem(), newLineEntities);
    copyFeatureProperties(target, newFeature, featureName, featureArea);
    return newFeature;
  }

  private void copyFeatureProperties(
      Feature target,
      Feature newFeature,
      String featureName,
      BigDecimal featureArea
  ) {
    newFeature.setFeatureName(featureName);
    newFeature.setCoordinateSystem(target.getCoordinateSystem());
    newFeature.setFeatureArea(featureArea);
    newFeature.setAttributes(new HashMap<>(target.getAttributes()));
    newFeature.setParentFeature(target.getParentFeature());
    newFeature.setStartDate(null);
    newFeature.setEndDate(null);
  }

  private Polygon buildPolygon(List<Polygon> inputPolygons, Feature newFeature) {
    var newPolygon = new Polygon();
    newPolygon.setFeature(newFeature);
    if (inputPolygons.size() == 1) {
      var inputPolygon = inputPolygons.getFirst();
      newPolygon.setAttributes(new HashMap<>(inputPolygon.getAttributes()));
      newPolygon.setStartDepth(inputPolygon.getStartDepth());
      newPolygon.setEndDepth(inputPolygon.getEndDepth());
    } else {
      //used when merging 2 polygons, we don't cascade parent attributes.
      newPolygon.setAttributes(new HashMap<>());
    }

    return newPolygon;
  }
}
