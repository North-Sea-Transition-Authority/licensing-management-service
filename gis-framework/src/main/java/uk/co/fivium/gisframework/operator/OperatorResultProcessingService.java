package uk.co.fivium.gisframework.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.LineNavigationType;

@Service
class OperatorResultProcessingService {

  private final PolygonService polygonService;
  private final LineService lineService;
  private final GrpcClientService grpcClientService;

  OperatorResultProcessingService(PolygonService polygonService,
                                  LineService lineService,
                                  GrpcClientService grpcClientService) {
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.grpcClientService = grpcClientService;
  }

  @Transactional
  public Feature processOutputPolygon(List<Feature> inputFeatures,
                                      String outputEsriJsonPolygon) {
    var inputPolygons = polygonService.getPolygons(inputFeatures);
    var inputPolygonLines = lineService.getLines(inputPolygons);

    var newLineEntities = buildLinesWithParentAttributes(outputEsriJsonPolygon, inputPolygonLines);
    //numberLines(newLineEntities); TODO next pr
    //validateLinesAreValid(newLineEntities, outputEsriJsonPolygon); TODO next pr
    //var newFeature = copyParentEntityAttributes(inputFeatures, inputPolygons, newLineEntities); TODO next pr
    lineService.saveLines(newLineEntities);
    //featureAreaService.calculateFeatureArea(newFeature); TODO next pr
    //return newFeature;
    return null;
  }

  /**
   * Creates new line entities for the output polygon, copying attributes from the parent line if a parent line can be
   * found for the output line. If no parent line can be found, a new line entity is created with no attributes.
   *
   * @param outputPolygonEsriJson the raw EsriJSON of the output polygons after a polygon operation.
   * @param inputPolygonLines the lines of the input polygons of the operation.
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
}
