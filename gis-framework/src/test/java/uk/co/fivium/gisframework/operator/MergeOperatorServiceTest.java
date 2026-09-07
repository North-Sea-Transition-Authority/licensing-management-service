package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@ExtendWith(MockitoExtension.class)
class MergeOperatorServiceTest {

  private static final String SQUARE =
      "{\"rings\":[[[0,0],[2,0],[2,2],[0,2],[0,0]]],\"spatialReference\":{\"wkid\":4326}}";
  private static final String SQUARE_WITH_MIDPOINT =
      "{\"rings\":[[[0,0],[1,0],[2,0],[2,2],[0,2],[0,0]]],\"spatialReference\":{\"wkid\":4326}}";

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private LineService lineService;

  @InjectMocks
  private MergeOperatorService mergeOperatorService;

  @Test
  void mergePolygons_whenNoInnerVertices_thenReturnsMergedFeatureUntouched() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();

    when(polygonService.getPolygonsAsEsriJson(featureInput1, false)).thenReturn(List.of("esri 1"));
    when(polygonService.getPolygonsAsEsriJson(featureInput2, false)).thenReturn(List.of("esri 2"));
    when(grpcClientService.mergePolygons("esri 1", "esri 2")).thenReturn(SQUARE);
    when(operatorResultProcessingService.processOutputPolygon(List.of(featureInput1, featureInput2), SQUARE, 1))
        .thenReturn(mergedFeature);
    when(grpcClientService.generalizePolygon(SQUARE)).thenReturn(SQUARE);

    var result = mergeOperatorService.mergePolygons(featureInput1, featureInput2);

    assertThat(result).isEqualTo(mergedFeature);
    verify(lineService, never()).deleteLines(any());
    verify(lineService, never()).saveLines(any());
  }

  @Test
  void mergePolygons_whenInnerVertexBetweenLinesWithMatchingAttributes_thenMergesThoseLines() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().build();

    var lineToInnerVertex = ringLine(polygon, 1, "[[0,0],[1,0]]", Map.of());
    var lineFromInnerVertex = ringLine(polygon, 2, "[[1,0],[2,0]]", Map.of());
    var line3 = ringLine(polygon, 3, "[[2,0],[2,2]]", Map.of());
    var line4 = ringLine(polygon, 4, "[[2,2],[0,2]]", Map.of());
    var line5 = ringLine(polygon, 5, "[[0,2],[0,0]]", Map.of());
    var combinedLineJson = "{\"paths\":[[[0,0],[2,0]]],\"spatialReference\":{\"wkid\":4326}}";

    when(polygonService.getPolygonsAsEsriJson(featureInput1, false)).thenReturn(List.of("esri 1"));
    when(polygonService.getPolygonsAsEsriJson(featureInput2, false)).thenReturn(List.of("esri 2"));
    when(grpcClientService.mergePolygons("esri 1", "esri 2")).thenReturn(SQUARE_WITH_MIDPOINT);
    when(operatorResultProcessingService.processOutputPolygon(
        List.of(featureInput1, featureInput2), SQUARE_WITH_MIDPOINT, 1)).thenReturn(mergedFeature);
    when(grpcClientService.generalizePolygon(SQUARE_WITH_MIDPOINT)).thenReturn(SQUARE);
    when(polygonService.findAllByFeature(mergedFeature)).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon)))
        .thenReturn(List.of(lineToInnerVertex, lineFromInnerVertex, line3, line4, line5));
    when(grpcClientService.mergeAndGeneralizeLines(
        List.of(lineToInnerVertex.getEsriJson(), lineFromInnerVertex.getEsriJson()))).thenReturn(combinedLineJson);

    var result = mergeOperatorService.mergePolygons(featureInput1, featureInput2);

    assertThat(result).isEqualTo(mergedFeature);
    assertThat(lineToInnerVertex.getEsriJson()).isEqualTo(combinedLineJson);

    var remainingLines = List.of(lineToInnerVertex, line3, line4, line5);
    verify(lineService).deleteLines(List.of(lineFromInnerVertex));
    verify(operatorResultProcessingService).numberLines(remainingLines);
    verify(operatorResultProcessingService).validateLinesAreValid(remainingLines, SQUARE_WITH_MIDPOINT);
    verify(lineService).saveLines(remainingLines);
  }

  @Test
  void mergePolygons_whenInnerVertexBetweenLinesWithDifferentAttributes_thenKeepsLinesSeparate() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().build();

    var lineToInnerVertex = ringLine(polygon, 1, "[[0,0],[1,0]]", Map.of("parent", "a"));
    var lineFromInnerVertex = ringLine(polygon, 2, "[[1,0],[2,0]]", Map.of("parent", "b"));
    var line3 = ringLine(polygon, 3, "[[2,0],[2,2]]", Map.of());
    var line4 = ringLine(polygon, 4, "[[2,2],[0,2]]", Map.of());
    var line5 = ringLine(polygon, 5, "[[0,2],[0,0]]", Map.of());

    when(polygonService.getPolygonsAsEsriJson(featureInput1, false)).thenReturn(List.of("esri 1"));
    when(polygonService.getPolygonsAsEsriJson(featureInput2, false)).thenReturn(List.of("esri 2"));
    when(grpcClientService.mergePolygons("esri 1", "esri 2")).thenReturn(SQUARE_WITH_MIDPOINT);
    when(operatorResultProcessingService.processOutputPolygon(
        List.of(featureInput1, featureInput2), SQUARE_WITH_MIDPOINT, 1)).thenReturn(mergedFeature);
    when(grpcClientService.generalizePolygon(SQUARE_WITH_MIDPOINT)).thenReturn(SQUARE);
    when(polygonService.findAllByFeature(mergedFeature)).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon)))
        .thenReturn(List.of(lineToInnerVertex, lineFromInnerVertex, line3, line4, line5));

    var result = mergeOperatorService.mergePolygons(featureInput1, featureInput2);

    assertThat(result).isEqualTo(mergedFeature);
    verify(grpcClientService, never()).mergeAndGeneralizeLines(any());
    verify(lineService, never()).deleteLines(any());
    verify(lineService, never()).saveLines(any());
  }

  private static Line ringLine(Polygon polygon, int displayOrder, String path, Map<String, Object> attributes) {
    return LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withRingNumber(1)
        .withDisplayOrder(displayOrder)
        .withEsriJson("{\"paths\":[" + path + "],\"spatialReference\":{\"wkid\":4326}}")
        .withAttributes(attributes)
        .build();
  }
}
