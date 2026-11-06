package uk.co.fivium.gisframework.operator.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.operator.EsriJsonPolygonWithSources;
import uk.co.fivium.gisframework.operator.EsriJsonPolygonWithSources.DepthRange;

@Service
public class MergeService {

  private final GrpcClientService grpcClientService;
  private final LineService lineService;

  MergeService(GrpcClientService grpcClientService, LineService lineService) {
    this.grpcClientService = grpcClientService;
    this.lineService = lineService;
  }

  List<EsriJsonPolygonWithSources> mergeWithDepths(List<EsriJsonPolygonWithSources> polygonsWithSources) {
    var allSources = polygonsWithSources.stream()
        .flatMap(polygonWithSources -> polygonWithSources.sources().stream())
        .toList();
    var sourceToLines = lineService.getLines(allSources).stream()
        .collect(Collectors.groupingBy(Line::getPolygon));

    List<EsriJsonPolygonWithSources> mergedPolygonsWithSources = new ArrayList<>(polygonsWithSources);
    boolean mergedSameDepthPolygons;
    do {
      mergeTopologicallyEqualPolygons(mergedPolygonsWithSources, sourceToLines);
      mergedSameDepthPolygons = mergeSameDepthPolygons(mergedPolygonsWithSources);
    } while (mergedSameDepthPolygons);
    return mergedPolygonsWithSources;
  }

  List<EsriJsonPolygonWithSources> mergeIn2d(List<EsriJsonPolygonWithSources> sameDepthPolygons) {
    var mergedEsriJson = grpcClientService.unionPolygons(
        sameDepthPolygons.stream()
            .map(EsriJsonPolygonWithSources::esriJson)
            .toList()
    );

    var singlePartPolygons = grpcClientService.multiPartToSinglePart(mergedEsriJson);
    if (singlePartPolygons.size() == 1) {
      return List.of(EsriJsonPolygonWithSources.combine(singlePartPolygons.getFirst(), sameDepthPolygons));
    }

    return singlePartPolygons.stream()
        .map(singlePartPolygon -> EsriJsonPolygonWithSources.combine(
            singlePartPolygon,
            polygonsContainedIn(singlePartPolygon, sameDepthPolygons)
        ))
        .toList();
  }

  private void mergeTopologicallyEqualPolygons(List<EsriJsonPolygonWithSources> polygonsWithSources,
                                               Map<Polygon, List<Line>> sourceToLines) {
    // Vertical merge needs meeting depths, so without any, grouping would change nothing
    if (!anyDepthsMeet(polygonsWithSources)) {
      return;
    }

    var topologicallyEqualPolygonGroups = findTopologicallyEqualPolygons(polygonsWithSources);
    polygonsWithSources.clear();
    for (var topologicallyEqualPolygons : topologicallyEqualPolygonGroups) {
      polygonsWithSources.addAll(verticalMerge(topologicallyEqualPolygons, sourceToLines));
    }
  }

  private List<List<EsriJsonPolygonWithSources>> findTopologicallyEqualPolygons(
      List<EsriJsonPolygonWithSources> polygonsWithSources
  ) {
    var idToEsriJson = polygonsWithSources.stream()
        .collect(Collectors.toMap(
            esriJsonPolygonWithSources -> esriJsonPolygonWithSources.id().toString(),
            EsriJsonPolygonWithSources::esriJson
        ));

    List<List<EsriJsonPolygonWithSources>> topologicallyEqualPolygonGroups = new ArrayList<>();
    Map<String, List<EsriJsonPolygonWithSources>> idToGroup = new HashMap<>();
    for (var ids : grpcClientService.groupEqualPolygons(idToEsriJson)) {
      List<EsriJsonPolygonWithSources> topologicallyEqualPolygons = new ArrayList<>();
      topologicallyEqualPolygonGroups.add(topologicallyEqualPolygons);
      ids.forEach(id -> idToGroup.put(id, topologicallyEqualPolygons));
    }

    for (var polygonWithSources : polygonsWithSources) {
      var group = idToGroup.get(polygonWithSources.id().toString());
      if (group == null) {
        throw new IllegalStateException("Polygon %s was not grouped".formatted(polygonWithSources.id()));
      }
      group.add(polygonWithSources);
    }
    return topologicallyEqualPolygonGroups;
  }

  private List<EsriJsonPolygonWithSources> verticalMerge(
      List<EsriJsonPolygonWithSources> topologicallyEqualPolygons,
      Map<Polygon, List<Line>> sourceToLines
  ) {
    List<EsriJsonPolygonWithSources> remaining = new ArrayList<>(topologicallyEqualPolygons);
    List<EsriJsonPolygonWithSources> mergedPolygons = new ArrayList<>();
    while (!remaining.isEmpty()) {
      var current = remaining.removeFirst();
      boolean mergedAny;
      do {
        mergedAny = false;
        var iterator = remaining.iterator();
        while (iterator.hasNext()) {
          var other = iterator.next();
          if (depthsMeet(current, other) && areLineAttributesEqual(current, other, sourceToLines)) {
            current = EsriJsonPolygonWithSources.combine(current.esriJson(), List.of(current, other));
            iterator.remove();
            mergedAny = true;
            break;
          }
        }
      } while (mergedAny);
      mergedPolygons.add(current);
    }
    return mergedPolygons;
  }

  private boolean mergeSameDepthPolygons(List<EsriJsonPolygonWithSources> polygonsWithSources) {
    boolean mergedAny = false;
    for (var sameDepthPolygons : groupByDepthRange(polygonsWithSources)) {
      if (sameDepthPolygons.size() > 1) {
        var mergedPolygons = mergeIn2d(sameDepthPolygons);
        polygonsWithSources.removeAll(sameDepthPolygons);
        polygonsWithSources.addAll(mergedPolygons);
        if (mergedPolygons.size() < sameDepthPolygons.size()) {
          mergedAny = true;
        }
      }
    }
    return mergedAny;
  }

  private List<EsriJsonPolygonWithSources> polygonsContainedIn(
      String polygon,
      List<EsriJsonPolygonWithSources> polygonsWithSources
  ) {
    return polygonsWithSources.stream()
        .filter(polygonWithSources -> grpcClientService.polygonContains(
            polygon,
            polygonWithSources.esriJson()
        ))
        .toList();
  }

  private List<List<EsriJsonPolygonWithSources>> groupByDepthRange(List<EsriJsonPolygonWithSources> polygonsWithSources) {
    Map<DepthRange, List<EsriJsonPolygonWithSources>> polygonsByDepthRange = new LinkedHashMap<>();
    for (var polygonWithSources : polygonsWithSources) {
      polygonsByDepthRange.computeIfAbsent(
          polygonWithSources.depthRange(),
          depthRange -> new ArrayList<>()
      ).add(polygonWithSources);
    }
    return new ArrayList<>(polygonsByDepthRange.values());
  }

  private boolean anyDepthsMeet(List<EsriJsonPolygonWithSources> polygons) {
    for (int i = 0; i < polygons.size(); i++) {
      for (int j = i + 1; j < polygons.size(); j++) {
        if (depthsMeet(polygons.get(i), polygons.get(j))) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean depthsMeet(EsriJsonPolygonWithSources firstPolygon, EsriJsonPolygonWithSources secondPolygon) {
    return firstPolygon.depthRange().meets(secondPolygon.depthRange());
  }

  private boolean areLineAttributesEqual(
      EsriJsonPolygonWithSources firstPolygon,
      EsriJsonPolygonWithSources secondPolygon,
      Map<Polygon, List<Line>> sourceToLines
  ) {
    var firstLines = linesOf(firstPolygon, sourceToLines);
    var secondLines = linesOf(secondPolygon, sourceToLines);
    var firstEsriJsonToAttributes = getEsriJsonToAttributes(firstLines);
    var secondEsriJsonToAttributes = getEsriJsonToAttributes(secondLines);

    return firstEsriJsonToAttributes.equals(secondEsriJsonToAttributes)
        || areLineAttributesEqualBySegment(firstPolygon, secondPolygon, firstLines, secondLines);
  }

  private List<Line> linesOf(EsriJsonPolygonWithSources polygon, Map<Polygon, List<Line>> sourceToLines) {
    return polygon.sources().stream()
        .flatMap(source -> sourceToLines.get(source).stream())
        .toList();
  }

  private Map<String, Map<String, Object>> getEsriJsonToAttributes(List<Line> lines) {
    return lines.stream()
        .collect(Collectors.toMap(Line::getEsriJson, Line::getAttributes, (existing, duplicate) -> existing));
  }

  private boolean areLineAttributesEqualBySegment(
      EsriJsonPolygonWithSources firstPolygon,
      EsriJsonPolygonWithSources secondPolygon,
      List<Line> firstLines,
      List<Line> secondLines
  ) {
    // We need to union the polygons first to get all the line segments, as they could be topologically equal but have different
    // vertices
    var segments = grpcClientService.explodePolygon(
        grpcClientService.unionPolygons(List.of(firstPolygon.esriJson(), secondPolygon.esriJson()))
    );
    Map<Map<String, Object>, UUID> attributesToGroupId = new HashMap<>();
    var firstGroups = grpcClientService.findParentLines(getEsriJsonToGroupId(firstLines, attributesToGroupId), segments);
    var secondGroups = grpcClientService.findParentLines(getEsriJsonToGroupId(secondLines, attributesToGroupId), segments);

    return firstGroups.orphanLines().isEmpty()
        && secondGroups.orphanLines().isEmpty()
        && firstGroups.equals(secondGroups);
  }

  private Map<String, UUID> getEsriJsonToGroupId(List<Line> lines, Map<Map<String, Object>, UUID> attributesToGroupId) {
    return lines.stream()
        .collect(Collectors.toMap(
            Line::getEsriJson,
            line -> attributesToGroupId.computeIfAbsent(line.getAttributes(), attributes -> UUID.randomUUID()),
            (existing, duplicate) -> existing,
            LinkedHashMap::new
        ));
  }
}
