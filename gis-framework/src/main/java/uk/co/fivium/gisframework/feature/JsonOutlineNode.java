package uk.co.fivium.gisframework.feature;

import com.esri.core.geometry.Point;

public record JsonOutlineNode(
    String polygonId,
    String lineId,
    Integer ringNumber,
    int displayOrder,
    double x,
    double y,
    String mapText
) {

  public JsonOutlineNode(Line line, int displayOrder, double x, double y, String mapText) {
    this(
        line.getPolygon().getId().toString(),
        line.getId().toString(),
        line.getRingNumber(),
        displayOrder,
        x,
        y,
        mapText
    );
  }

  public JsonOutlineNode(Line line, int displayOrder, Point point) {
    this(
        line.getPolygon().getId().toString(),
        line.getId().toString(),
        line.getRingNumber(),
        displayOrder,
        point.getX(),
        point.getY(),
        "(%s)".formatted(displayOrder)
    );
  }

  JsonOutlineNode withMapText(String mapText) {
    return new JsonOutlineNode(polygonId, lineId, ringNumber, displayOrder, x, y, mapText);
  }
}
