package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
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
  private static final String RECTANGLE_WITH_CORNER_AT_MIDPOINT =
      "{\"rings\":[[[0,0],[1,0],[1,2],[0,2],[0,0]]],\"spatialReference\":{\"wkid\":4326}}";

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @Mock
  private FeatureService featureService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private LineService lineService;

  @Mock
  private MergeService mergeService;

  @InjectMocks
  private MergeOperatorService mergeOperatorService;

  @Test
  void mergePolygons_whenFewerThanTwoInputFeatures_thenThrows() {
    var singleFeature = List.of(FeatureTestUtil.newBuilder().build());

    assertThatThrownBy(() -> mergeOperatorService.mergePolygons(singleFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Must provide at least two input features");
  }

  @Test
  void mergePolygons_whenNoPolygonsHaveDepth_thenMergesIn2d() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var inputPolygon1 = PolygonTestUtil.newBuilder()
        .withFeature(featureInput1)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();
    var inputPolygon2 = PolygonTestUtil.newBuilder()
        .withFeature(featureInput2)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var mergedPolygon1 = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var mergedPolygon2 = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var mergedPolygonsWithSources = List.of(
        new EsriJsonPolygonWithSources(SQUARE, List.of(inputPolygon1)),
        new EsriJsonPolygonWithSources(RECTANGLE_WITH_CORNER_AT_MIDPOINT, List.of(inputPolygon2))
    );

    stubInputFeature(featureInput1, Map.of(inputPolygon1, "esri 1"));
    stubInputFeature(featureInput2, Map.of(inputPolygon2, "esri 2"));
    when(mergeService.mergeIn2d(List.of(
        new EsriJsonPolygonWithSources("esri 1", List.of(inputPolygon1)),
        new EsriJsonPolygonWithSources("esri 2", List.of(inputPolygon2))
    ))).thenReturn(mergedPolygonsWithSources);
    when(operatorResultProcessingService.processOutputPolygons(
        List.of(featureInput1, featureInput2), mergedPolygonsWithSources)).thenReturn(mergedFeature);
    stubMergedFeature(
        mergedFeature,
        Map.of(mergedPolygon1, List.of(), mergedPolygon2, List.of()),
        Map.of(mergedPolygon1, SQUARE, mergedPolygon2, RECTANGLE_WITH_CORNER_AT_MIDPOINT)
    );
    when(grpcClientService.generalizePolygon(SQUARE)).thenReturn(SQUARE);
    when(grpcClientService.generalizePolygon(RECTANGLE_WITH_CORNER_AT_MIDPOINT))
        .thenReturn(RECTANGLE_WITH_CORNER_AT_MIDPOINT);

    var result = mergeOperatorService.mergePolygons(List.of(featureInput1, featureInput2));

    assertThat(result).isEqualTo(mergedFeature);
    verify(mergeService, never()).mergeWithDepths(any());
  }

  @ParameterizedTest
  @CsvSource({"-100,", ",-100", "0,-100"})
  void mergePolygons_whenAnyPolygonHasDepth_thenMergesWithDepth(Long startDepth, Long endDepth) {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var polygonWithoutDepth = PolygonTestUtil.newBuilder()
        .withFeature(featureInput1)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();
    var polygonWithDepth = PolygonTestUtil.newBuilder()
        .withFeature(featureInput2)
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var mergedPolygon = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var mergedPolygonsWithSources = List.of(
        new EsriJsonPolygonWithSources(SQUARE, List.of(polygonWithoutDepth, polygonWithDepth))
    );

    stubMergeOfTwoInputs(
        featureInput1,
        polygonWithoutDepth,
        featureInput2,
        polygonWithDepth,
        mergedPolygonsWithSources
    );
    when(operatorResultProcessingService.processOutputPolygons(
        List.of(featureInput1, featureInput2), mergedPolygonsWithSources)).thenReturn(mergedFeature);
    stubMergedFeature(mergedFeature, Map.of(mergedPolygon, List.of()), Map.of(mergedPolygon, SQUARE));
    when(grpcClientService.generalizePolygon(SQUARE)).thenReturn(SQUARE);

    var result = mergeOperatorService.mergePolygons(List.of(featureInput1, featureInput2));

    assertThat(result).isEqualTo(mergedFeature);
    verify(mergeService, never()).mergeIn2d(any());
  }

  @Test
  void mergePolygons_whenNoInnerVertices_thenPassesEachInputPolygonToCalculatorAndDeletesNoLines() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var inputPolygon1 = PolygonTestUtil.newBuilder().withFeature(featureInput1).build();
    var inputPolygon2 = PolygonTestUtil.newBuilder().withFeature(featureInput2).build();
    var inputPolygon3 = PolygonTestUtil.newBuilder().withFeature(featureInput2).build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var mergedPolygon = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var mergedPolygonsWithSources = List.of(
        new EsriJsonPolygonWithSources(SQUARE, List.of(inputPolygon1, inputPolygon2, inputPolygon3))
    );

    stubInputFeature(featureInput1, Map.of(inputPolygon1, "esri 1"));
    var input2PolygonToEsriJson = new LinkedHashMap<Polygon, String>();
    input2PolygonToEsriJson.put(inputPolygon2, "esri 2");
    input2PolygonToEsriJson.put(inputPolygon3, "esri 3");
    stubInputFeature(featureInput2, input2PolygonToEsriJson);
    when(mergeService.mergeWithDepths(List.of(
        new EsriJsonPolygonWithSources("esri 1", List.of(inputPolygon1)),
        new EsriJsonPolygonWithSources("esri 2", List.of(inputPolygon2)),
        new EsriJsonPolygonWithSources("esri 3", List.of(inputPolygon3))
    ))).thenReturn(mergedPolygonsWithSources);
    when(operatorResultProcessingService.processOutputPolygons(
        List.of(featureInput1, featureInput2), mergedPolygonsWithSources)).thenReturn(mergedFeature);
    stubMergedFeature(mergedFeature, Map.of(mergedPolygon, List.of()), Map.of(mergedPolygon, SQUARE));
    when(grpcClientService.generalizePolygon(SQUARE)).thenReturn(SQUARE);

    var result = mergeOperatorService.mergePolygons(List.of(featureInput1, featureInput2));

    assertThat(result).isEqualTo(mergedFeature);
    verify(lineService, never()).deleteLines(any());
    verify(lineService, never()).saveLines(any());
  }

  @Test
  void mergePolygons_whenInnerVertexBetweenLinesWithMatchingAttributes_thenMergesThoseLines() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var inputPolygon1 = PolygonTestUtil.newBuilder().withFeature(featureInput1).build();
    var inputPolygon2 = PolygonTestUtil.newBuilder().withFeature(featureInput2).build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var mergedPolygon = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var mergedPolygonsWithSources = List.of(
        new EsriJsonPolygonWithSources(SQUARE_WITH_MIDPOINT, List.of(inputPolygon1, inputPolygon2))
    );

    var lineToInnerVertex = ringLine(mergedPolygon, 1, "[[0,0],[1,0]]", Map.of());
    var lineFromInnerVertex = ringLine(mergedPolygon, 2, "[[1,0],[2,0]]", Map.of());
    var line3 = ringLine(mergedPolygon, 3, "[[2,0],[2,2]]", Map.of());
    var line4 = ringLine(mergedPolygon, 4, "[[2,2],[0,2]]", Map.of());
    var line5 = ringLine(mergedPolygon, 5, "[[0,2],[0,0]]", Map.of());
    var combinedLineJson = "{\"paths\":[[[0,0],[2,0]]],\"spatialReference\":{\"wkid\":4326}}";

    stubMergeOfTwoInputs(featureInput1, inputPolygon1, featureInput2, inputPolygon2, mergedPolygonsWithSources);
    when(operatorResultProcessingService.processOutputPolygons(
        List.of(featureInput1, featureInput2), mergedPolygonsWithSources)).thenReturn(mergedFeature);
    stubMergedFeature(
        mergedFeature,
        Map.of(mergedPolygon, List.of(lineToInnerVertex, lineFromInnerVertex, line3, line4, line5)),
        Map.of(mergedPolygon, SQUARE_WITH_MIDPOINT)
    );
    when(grpcClientService.generalizePolygon(SQUARE_WITH_MIDPOINT)).thenReturn(SQUARE);
    when(grpcClientService.mergeAndGeneralizeLines(
        List.of(lineToInnerVertex.getEsriJson(), lineFromInnerVertex.getEsriJson()))).thenReturn(combinedLineJson);

    var result = mergeOperatorService.mergePolygons(List.of(featureInput1, featureInput2));

    assertThat(result).isEqualTo(mergedFeature);
    assertThat(lineToInnerVertex.getEsriJson()).isEqualTo(combinedLineJson);

    var remainingLines = List.of(lineToInnerVertex, line3, line4, line5);
    verify(lineService).deleteLines(List.of(lineFromInnerVertex));
    verify(operatorResultProcessingService).numberLines(remainingLines);
    verify(operatorResultProcessingService).validateLinesAreValid(remainingLines, List.of(SQUARE_WITH_MIDPOINT));
    verify(lineService).saveLines(remainingLines);
  }

  @Test
  void mergePolygons_whenInnerVertexBetweenLinesWithDifferentAttributes_thenKeepsLinesSeparate() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var inputPolygon1 = PolygonTestUtil.newBuilder().withFeature(featureInput1).build();
    var inputPolygon2 = PolygonTestUtil.newBuilder().withFeature(featureInput2).build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var mergedPolygon = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var mergedPolygonsWithSources = List.of(
        new EsriJsonPolygonWithSources(SQUARE_WITH_MIDPOINT, List.of(inputPolygon1, inputPolygon2))
    );

    var lineToInnerVertex = ringLine(mergedPolygon, 1, "[[0,0],[1,0]]", Map.of("parent", "a"));
    var lineFromInnerVertex = ringLine(mergedPolygon, 2, "[[1,0],[2,0]]", Map.of("parent", "b"));
    var line3 = ringLine(mergedPolygon, 3, "[[2,0],[2,2]]", Map.of());
    var line4 = ringLine(mergedPolygon, 4, "[[2,2],[0,2]]", Map.of());
    var line5 = ringLine(mergedPolygon, 5, "[[0,2],[0,0]]", Map.of());

    stubMergeOfTwoInputs(featureInput1, inputPolygon1, featureInput2, inputPolygon2, mergedPolygonsWithSources);
    when(operatorResultProcessingService.processOutputPolygons(
        List.of(featureInput1, featureInput2), mergedPolygonsWithSources)).thenReturn(mergedFeature);
    stubMergedFeature(
        mergedFeature,
        Map.of(mergedPolygon, List.of(lineToInnerVertex, lineFromInnerVertex, line3, line4, line5)),
        Map.of(mergedPolygon, SQUARE_WITH_MIDPOINT)
    );
    when(grpcClientService.generalizePolygon(SQUARE_WITH_MIDPOINT)).thenReturn(SQUARE);

    var result = mergeOperatorService.mergePolygons(List.of(featureInput1, featureInput2));

    assertThat(result).isEqualTo(mergedFeature);
    verify(grpcClientService, never()).mergeAndGeneralizeLines(any());
    verify(lineService, never()).deleteLines(any());
    verify(lineService, never()).saveLines(any());
  }

  @Test
  void mergePolygons_whenInnerVertexIsRealCornerOfAnotherPolygon_thenOnlyMergesLinesOfPolygonWithInnerVertex() {
    var featureInput1 = FeatureTestUtil.newBuilder().build();
    var featureInput2 = FeatureTestUtil.newBuilder().build();
    var inputPolygon1 = PolygonTestUtil.newBuilder().withFeature(featureInput1).build();
    var inputPolygon2 = PolygonTestUtil.newBuilder().withFeature(featureInput2).build();
    var mergedFeature = FeatureTestUtil.newBuilder().build();
    var polygonWithInnerVertex = PolygonTestUtil.newBuilder().withFeature(mergedFeature).build();
    var polygonWithCorner = PolygonTestUtil.newBuilder()
        .withFeature(mergedFeature)
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();
    var mergedPolygonsWithSources = List.of(
        new EsriJsonPolygonWithSources(SQUARE_WITH_MIDPOINT, List.of(inputPolygon1)),
        new EsriJsonPolygonWithSources(RECTANGLE_WITH_CORNER_AT_MIDPOINT, List.of(inputPolygon2))
    );

    var lineToInnerVertex = ringLine(polygonWithInnerVertex, 1, "[[0,0],[1,0]]", Map.of());
    var lineFromInnerVertex = ringLine(polygonWithInnerVertex, 2, "[[1,0],[2,0]]", Map.of());
    var line3 = ringLine(polygonWithInnerVertex, 3, "[[2,0],[2,2]]", Map.of());
    var line4 = ringLine(polygonWithInnerVertex, 4, "[[2,2],[0,2]]", Map.of());
    var line5 = ringLine(polygonWithInnerVertex, 5, "[[0,2],[0,0]]", Map.of());
    var lineToCorner = ringLine(polygonWithCorner, 6, "[[0,0],[1,0]]", Map.of());
    var lineFromCorner = ringLine(polygonWithCorner, 7, "[[1,0],[1,2]]", Map.of());
    var line8 = ringLine(polygonWithCorner, 8, "[[1,2],[0,2]]", Map.of());
    var line9 = ringLine(polygonWithCorner, 9, "[[0,2],[0,0]]", Map.of());
    var combinedLineJson = "{\"paths\":[[[0,0],[2,0]]],\"spatialReference\":{\"wkid\":4326}}";

    var mergedPolygonToLines = new LinkedHashMap<Polygon, List<Line>>();
    mergedPolygonToLines.put(
        polygonWithInnerVertex,
        List.of(lineToInnerVertex, lineFromInnerVertex, line3, line4, line5)
    );
    mergedPolygonToLines.put(polygonWithCorner, List.of(lineToCorner, lineFromCorner, line8, line9));

    stubMergeOfTwoInputs(featureInput1, inputPolygon1, featureInput2, inputPolygon2, mergedPolygonsWithSources);
    when(operatorResultProcessingService.processOutputPolygons(
        List.of(featureInput1, featureInput2), mergedPolygonsWithSources)).thenReturn(mergedFeature);
    stubMergedFeature(
        mergedFeature,
        mergedPolygonToLines,
        Map.of(
            polygonWithInnerVertex, SQUARE_WITH_MIDPOINT,
            polygonWithCorner, RECTANGLE_WITH_CORNER_AT_MIDPOINT
        )
    );
    when(grpcClientService.generalizePolygon(SQUARE_WITH_MIDPOINT)).thenReturn(SQUARE);
    when(grpcClientService.generalizePolygon(RECTANGLE_WITH_CORNER_AT_MIDPOINT))
        .thenReturn(RECTANGLE_WITH_CORNER_AT_MIDPOINT);
    when(grpcClientService.mergeAndGeneralizeLines(
        List.of(lineToInnerVertex.getEsriJson(), lineFromInnerVertex.getEsriJson()))).thenReturn(combinedLineJson);

    var result = mergeOperatorService.mergePolygons(List.of(featureInput1, featureInput2));

    assertThat(result).isEqualTo(mergedFeature);

    var remainingLines = List.of(lineToInnerVertex, line3, line4, line5, lineToCorner, lineFromCorner, line8, line9);
    verify(lineService).deleteLines(List.of(lineFromInnerVertex));
    verify(operatorResultProcessingService).numberLines(remainingLines);
    verify(operatorResultProcessingService).validateLinesAreValid(
        remainingLines,
        List.of(SQUARE_WITH_MIDPOINT, RECTANGLE_WITH_CORNER_AT_MIDPOINT)
    );
    verify(lineService).saveLines(remainingLines);
  }

  private void stubMergeOfTwoInputs(
      Feature featureInput1,
      Polygon inputPolygon1,
      Feature featureInput2,
      Polygon inputPolygon2,
      List<EsriJsonPolygonWithSources> mergedPolygonsWithSources
  ) {
    stubInputFeature(featureInput1, Map.of(inputPolygon1, "esri 1"));
    stubInputFeature(featureInput2, Map.of(inputPolygon2, "esri 2"));
    when(mergeService.mergeWithDepths(List.of(
        new EsriJsonPolygonWithSources("esri 1", List.of(inputPolygon1)),
        new EsriJsonPolygonWithSources("esri 2", List.of(inputPolygon2))
    ))).thenReturn(mergedPolygonsWithSources);
  }

  private void stubInputFeature(Feature feature, Map<Polygon, String> polygonToEsriJson) {
    var polygonToLines = new LinkedHashMap<Polygon, List<Line>>();
    var polygonIdToEsriJson = new LinkedHashMap<UUID, String>();
    polygonToEsriJson.forEach((polygon, esriJson) -> {
      polygonToLines.put(polygon, List.of());
      polygonIdToEsriJson.put(polygon.getId(), esriJson);
    });
    var entityBackedFeature = new EntityBackedFeature(feature, polygonToLines);

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(polygonIdToEsriJson);
  }

  private void stubMergedFeature(
      Feature mergedFeature,
      Map<Polygon, List<Line>> polygonToLines,
      Map<Polygon, String> polygonToEsriJson
  ) {
    var entityBackedFeature = new EntityBackedFeature(mergedFeature, polygonToLines);
    var polygonIdToEsriJson = new LinkedHashMap<UUID, String>();
    polygonToEsriJson.forEach((polygon, esriJson) -> polygonIdToEsriJson.put(polygon.getId(), esriJson));

    when(featureService.getEntityBackedFeature(mergedFeature)).thenReturn(entityBackedFeature);
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(polygonIdToEsriJson);
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
