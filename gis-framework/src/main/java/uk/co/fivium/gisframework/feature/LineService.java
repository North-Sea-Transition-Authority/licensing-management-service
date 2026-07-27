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
    return getOrderedLinesByFeatureId(features, true).entrySet()
        .stream()
        .map(entry -> new JsonFeatureOutlineNodes(
            entry.getKey().toString(),
            buildJsonOutlineNodes(entry.getValue())))
        .toList();
  }

  /**
   * Fetches the lines of the given features (with their start/end points) grouped by feature id and ordered
   * within each feature by display order. Shared by outline-node labelling and textual descriptions so both use
   * an identical ordering; {@code projectToWgs84} selects WGS84 map coordinates or the feature's native coordinates.
   */
  Map<UUID, List<LineWithStartEndPoints>> getOrderedLinesByFeatureId(
      Collection<Feature> features,
      boolean projectToWgs84
  ) {
    return grpcClientService.getLineStartAndEndPoints(lineRepository.findAllByPolygon_FeatureIn(features), projectToWgs84)
        .stream()
        .collect(Collectors.groupingBy(
            wrapper -> wrapper.line().getPolygon().getFeature().getId(),
            Collectors.collectingAndThen(
                Collectors.toList(),
                wrappers -> wrappers.stream()
                    .sorted(Comparator.comparing(wrapper -> wrapper.line().getDisplayOrder()))
                    .toList())));
  }

  /**
   * Assigns each ring's coordinates a continuous display number across the feature. Each line contributes its
   * start node; the closing line of a ring additionally contributes the ring's coincident end node (which is why
   * the running number is offset by the number of rings already closed).
   */
  List<NumberedNode> getOutlineNumberedNodes(List<LineWithStartEndPoints> orderedLines) {
    var nodes = new ArrayList<NumberedNode>();
    int ringCounter = 0;

    for (int i = 0; i < orderedLines.size(); i++) {
      var lineWrapper = orderedLines.get(i);
      var line = lineWrapper.line();
      int startDisplayOrder = line.getDisplayOrder() + ringCounter;

      nodes.add(new NumberedNode(line, startDisplayOrder, lineWrapper.start().getX(), lineWrapper.start().getY(), false));

      if (isRingBoundary(i, orderedLines)) {
        nodes.add(new NumberedNode(line, startDisplayOrder + 1, lineWrapper.end().getX(), lineWrapper.end().getY(), true));
        ringCounter++;
      }
    }

    return nodes;
  }

  private List<JsonOutlineNode> buildJsonOutlineNodes(List<LineWithStartEndPoints> orderedLines) {
    var allNodes = getOutlineNumberedNodes(orderedLines).stream()
        .map(node -> new JsonOutlineNode(
            node.line(),
            node.displayOrder(),
            node.x(),
            node.y(),
            "(%s)".formatted(node.displayOrder())
            )
        )
        .toList();

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
