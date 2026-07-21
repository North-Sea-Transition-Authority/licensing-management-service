package uk.co.fivium.gisframework.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.operator.LineWithStartEndPoints;

@Service
public class LineService {

  private final LineRepository lineRepository;
  private final GrpcClientService grpcClientService;

  public LineService(
      LineRepository lineRepository,
      GrpcClientService grpcClientService
  ) {
    this.lineRepository = lineRepository;
    this.grpcClientService = grpcClientService;
  }

  @Transactional
  public void saveLine(Line line) {
    lineRepository.save(line);
  }

  @Transactional
  public void saveLines(Collection<Line> lines) {
    lineRepository.saveAll(lines);
  }

  public List<Line> findAllByFeatureIn(Collection<Feature> features) {
    return lineRepository.findAllByPolygon_FeatureIn(features);
  }

  public List<Line> findAllByFeatureLegacyIdIn(Collection<Integer> featureLegacyIds) {
    return lineRepository.findAllByPolygon_Feature_LegacyIdIn(featureLegacyIds);
  }

  public List<Line> getLines(List<Polygon> polygons) {
    return lineRepository.findAllByPolygonIn(polygons);
  }

  public Map<Polygon, List<Line>> getPolygonToLines(Feature feature) {
    return lineRepository.findAllByPolygon_Feature(feature)
        .stream()
        .collect(Collectors.groupingBy(Line::getPolygon));
  }

  public Map<Polygon, List<Line>> getPolygonToLinesIn(Collection<Feature> features) {
    return lineRepository.findAllByPolygon_FeatureIn(features)
        .stream()
        .collect(Collectors.groupingBy(Line::getPolygon));
  }

  public List<JsonFeatureOutlineNodes> getOutlineNodes(Collection<Feature> features) {
    Map<UUID, List<LineWithStartEndPoints>> featureToOrderedLines =
        grpcClientService.getLineStartAndEndPoints(lineRepository.findAllByPolygon_FeatureIn(features), true)
        .stream()
        .collect(Collectors.groupingBy(
            wrapper -> wrapper.line().getPolygon().getFeature().getId(),
            Collectors.collectingAndThen(
                Collectors.toList(),
                wrappers -> wrappers.stream()
                    .sorted(Comparator.comparing(wrapper -> wrapper.line().getDisplayOrder()))
                    .toList())));

    return featureToOrderedLines.entrySet()
        .stream()
        .map(entry -> new JsonFeatureOutlineNodes(
            entry.getKey().toString(),
            buildOutlineNodes(entry.getValue())))
        .toList();
  }

  private List<JsonOutlineNode> buildOutlineNodes(List<LineWithStartEndPoints> orderedLines) {
    var allNodes = new ArrayList<JsonOutlineNode>();
    int ringCounter = 0;

    for (int i = 0; i < orderedLines.size(); i++) {
      var lineWrapper = orderedLines.get(i);
      var line = lineWrapper.line();
      int startDisplayOrder = line.getDisplayOrder() + ringCounter;

      allNodes.add(new JsonOutlineNode(line, startDisplayOrder, lineWrapper.start()));

      if (isRingBoundary(i, orderedLines)) {
        int endDisplayOrder = startDisplayOrder + 1;
        allNodes.add(new JsonOutlineNode(line, endDisplayOrder, lineWrapper.end()));
        ringCounter++;
      }
    }

    var ringCoordinateToNodes = new LinkedHashMap<String, List<JsonOutlineNode>>();
    for (var node : allNodes) {
      ringCoordinateToNodes
          .computeIfAbsent(ringCoordinateKey(node), key -> new ArrayList<>())
          .add(node);
    }

    var coordinateToMapText = new LinkedHashMap<String, String>();
    ringCoordinateToNodes.forEach((key, nodes) -> coordinateToMapText.put(key, "(%s)".formatted(
        nodes.stream()
            .map(JsonOutlineNode::displayOrder)
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(", ")))));

    return allNodes.stream()
        .map(node -> node.withMapText(coordinateToMapText.get(ringCoordinateKey(node))))
        .toList();
  }

  private static String ringCoordinateKey(JsonOutlineNode node) {
    return "%s|%s|%s,%s".formatted(node.polygonId(), node.ringNumber(), node.x(), node.y());
  }

  private boolean isRingBoundary(int lineIndex, List<LineWithStartEndPoints> orderedLines) {
    if (lineIndex == orderedLines.size() - 1) {
      return true;
    }
    var current = orderedLines.get(lineIndex).line();
    var next = orderedLines.get(lineIndex + 1).line();
    return !Objects.equals(current.getRingNumber(), next.getRingNumber())
        || !Objects.equals(current.getPolygon().getId(), next.getPolygon().getId());
  }

  @Transactional
  public void deleteAll() {
    lineRepository.deleteAll();
  }
}
