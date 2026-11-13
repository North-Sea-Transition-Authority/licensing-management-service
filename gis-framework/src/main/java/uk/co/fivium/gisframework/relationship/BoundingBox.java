package uk.co.fivium.gisframework.relationship;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public record BoundingBox(
    double minX,
    double minY,
    double maxX,
    double maxY
) {

  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  static BoundingBox fromEsriJsonPolyline(String esriJsonPolyline) {
    JsonNode paths;
    try {
      paths = OBJECT_MAPPER.readTree(esriJsonPolyline).path("paths");
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Unable to parse EsriJSON polyline %s".formatted(esriJsonPolyline), e);
    }

    var xs = new DoubleSummaryStatistics();
    var ys = new DoubleSummaryStatistics();
    paths.forEach(path -> path.forEach(point -> {
      xs.accept(point.get(0).asDouble());
      ys.accept(point.get(1).asDouble());
    }));

    if (xs.getCount() == 0) {
      throw new IllegalArgumentException("EsriJSON polyline %s has no points".formatted(esriJsonPolyline));
    }

    return new BoundingBox(xs.getMin(), ys.getMin(), xs.getMax(), ys.getMax());
  }

  static BoundingBox enclosing(Collection<BoundingBox> boundingBoxes) {
    return new BoundingBox(
        boundingBoxes.stream().mapToDouble(BoundingBox::minX).min().orElseThrow(),
        boundingBoxes.stream().mapToDouble(BoundingBox::minY).min().orElseThrow(),
        boundingBoxes.stream().mapToDouble(BoundingBox::maxX).max().orElseThrow(),
        boundingBoxes.stream().mapToDouble(BoundingBox::maxY).max().orElseThrow()
    );
  }

  boolean intersects(BoundingBox other) {
    return minX <= other.maxX && other.minX <= maxX && minY <= other.maxY && other.minY <= maxY;
  }
}
