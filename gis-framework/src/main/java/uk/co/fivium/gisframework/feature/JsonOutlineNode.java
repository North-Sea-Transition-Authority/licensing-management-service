package uk.co.fivium.gisframework.feature;

public record JsonOutlineNode(
    String polygonId,
    String lineId,
    Integer ringNumber,
    int displayOrder,
    double x,
    double y
) {
  public JsonOutlineNode(Line line, int displayOrder, double x, double y) {
    this(
        line.getPolygon().getId().toString(),
        line.getId().toString(),
        line.getRingNumber(),
        displayOrder,
        x,
        y
    );
  }
}
