package uk.co.fivium.gisframework.relationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BoundingBoxTest {

  @Test
  void fromEsriJsonPolyline_whenSinglePath_thenBoundsOfItsPoints() {
    var esriJson = """
        {"spatialReference":{"wkid":4230},"paths":[[[2.8,53.95],[2.5,54.1],[2.9,53.9]]]}
        """;

    var result = BoundingBox.fromEsriJsonPolyline(esriJson);

    assertThat(result).isEqualTo(new BoundingBox(2.5, 53.9, 2.9, 54.1));
  }

  @Test
  void fromEsriJsonPolyline_whenSeveralPaths_thenBoundsOfPointsInEveryPath() {
    var esriJson = """
        {"spatialReference":{"wkid":27700},"paths":[[[0,0],[1,1]],[[-3,2],[5,-4]]]}
        """;

    var result = BoundingBox.fromEsriJsonPolyline(esriJson);

    assertThat(result).isEqualTo(new BoundingBox(-3, -4, 5, 2));
  }

  @Test
  void fromEsriJsonPolyline_whenNoPoints_thenThrowsException() {
    var esriJson = """
        {"spatialReference":{"wkid":27700},"paths":[]}""";

    assertThatThrownBy(() -> BoundingBox.fromEsriJsonPolyline(esriJson))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("EsriJSON polyline {\"spatialReference\":{\"wkid\":27700},\"paths\":[]} has no points");
  }

  @Test
  void fromEsriJsonPolyline_whenNotJson_thenThrowsException() {
    assertThatThrownBy(() -> BoundingBox.fromEsriJsonPolyline("not json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unable to parse EsriJSON polyline not json");
  }

  @Test
  void enclosing() {
    var boundingBoxes = List.of(
        new BoundingBox(0, 0, 1, 1),
        new BoundingBox(-2, 3, 4, 5),
        new BoundingBox(1, -6, 2, 0)
    );

    var result = BoundingBox.enclosing(boundingBoxes);

    assertThat(result).isEqualTo(new BoundingBox(-2, -6, 4, 5));
  }

  @ParameterizedTest
  @MethodSource("intersectingBoundingBoxes")
  void intersects_whenBoundingBoxesShareAnyPoint_thenTrue(BoundingBox other) {
    var boundingBox = new BoundingBox(0, 0, 10, 10);

    assertThat(boundingBox.intersects(other)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("separateBoundingBoxes")
  void intersects_whenBoundingBoxesShareNoPoint_thenFalse(BoundingBox other) {
    var boundingBox = new BoundingBox(0, 0, 10, 10);

    assertThat(boundingBox.intersects(other)).isFalse();
  }

  private static List<Arguments> intersectingBoundingBoxes() {
    return List.of(
        Arguments.of(new BoundingBox(5, 5, 15, 15)),
        Arguments.of(new BoundingBox(2, 2, 4, 4)),
        Arguments.of(new BoundingBox(-5, -5, 15, 15)),
        Arguments.of(new BoundingBox(-5, 4, 15, 6)),
        Arguments.of(new BoundingBox(10, 2, 14, 6)),
        Arguments.of(new BoundingBox(10, 10, 14, 14))
    );
  }

  private static List<Arguments> separateBoundingBoxes() {
    return List.of(
        Arguments.of(new BoundingBox(11, 0, 15, 10)),
        Arguments.of(new BoundingBox(-5, 0, -1, 10)),
        Arguments.of(new BoundingBox(0, 11, 10, 15)),
        Arguments.of(new BoundingBox(0, -5, 10, -1)),
        Arguments.of(new BoundingBox(11, 11, 15, 15))
    );
  }
}
