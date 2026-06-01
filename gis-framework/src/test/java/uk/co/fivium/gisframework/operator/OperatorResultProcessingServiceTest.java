package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esri.core.geometry.Point;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.FindParentLineResponse;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;

@ExtendWith(MockitoExtension.class)
class OperatorResultProcessingServiceTest {

  @Mock
  private PolygonService polygonService;

  @Mock
  private LineService lineService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private FeatureService featureService;

  @Captor
  private ArgumentCaptor<List<Line>> linesCaptor;

  @Captor
  private ArgumentCaptor<Feature> featureCaptor;

  @Captor
  private ArgumentCaptor<Polygon> polygonCaptor;

  @InjectMocks
  private OperatorResultProcessingService operatorResultProcessingService;

  private static final Date START_DATE = Date.from(LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
  private static final Map<String, Object> FEATURE_ATTRIBUTES = Map.of("KEY", "VALUE");
  private static final Feature FEATURE = FeatureTestUtil.newBuilder()
      .withFeatureName("Test Feature")
      .withCoordinateSystem(CoordinateSystem.ED50)
      .withAttributes(FEATURE_ATTRIBUTES)
      .withFeatureArea(BigDecimal.TEN)
      .withStartDate(START_DATE)
      .withEndDate(null)
      .build();

  @Test
  void processOutputPolygon_whenParentLineFound_savesLineWithCopiedParentAttributesAndNavigationType() {
    var polygon = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var outputPolygonEsriJson = "output polygon";
    var outputLineEsriJson = "output line";
    var parentLineId = UUID.randomUUID();
    var parentAttributes = Map.<String, Object>of("source", "parent line");
    var parentLine = LineTestUtil.newBuilder()
        .withId(parentLineId)
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.CARTESIAN)
        .withAttributes(parentAttributes)
        .build();

    when(polygonService.getPolygons(List.of(FEATURE))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);

    operatorResultProcessingService.processOutputPolygon(List.of(FEATURE), outputPolygonEsriJson, 1);

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .extracting(
            Line::getEsriJson,
            Line::getAttributes,
            Line::getNavigationType
        )
        .containsExactly(
            outputLineEsriJson,
            parentAttributes,
            LineNavigationType.CARTESIAN
        );
  }

  @Test
  void processOutputPolygon_whenOrphanLineAndAnyInputLineIsCartesian_savesOrphanAsCartesianWithEmptyAttributes() {
    var polygon = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var outputPolygonEsriJson = "output polygon";
    var orphanLineEsriJson = "orphan line";
    var parentAttributes = Map.<String, Object>of("source", "parent line");
    var inputLine = LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.CARTESIAN)
        .withAttributes(parentAttributes)
        .build();

    when(polygonService.getPolygons(List.of(FEATURE))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(orphanLineEsriJson));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(orphanLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(orphanLineEsriJson)));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);

    operatorResultProcessingService.processOutputPolygon(List.of(FEATURE), outputPolygonEsriJson, 1);

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .extracting(
            Line::getEsriJson,
            Line::getAttributes,
            Line::getNavigationType
        )
        .containsExactly(
            orphanLineEsriJson,
            Collections.emptyMap(),
            LineNavigationType.CARTESIAN
        );
  }

  @Test
  void processOutputPolygon_whenOrphanLineAndNoInputLineIsCartesian_savesOrphanAsLoxodromeWithEmptyAttributes() {
    var polygon = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var outputPolygonEsriJson = "output polygon";
    var orphanLineEsriJson = "orphan line";
    var parentAttributes = Map.<String, Object>of("source", "parent line");
    var inputLine = LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withNavigationType(LineNavigationType.LOXODROME)
        .withAttributes(parentAttributes)
        .build();

    when(polygonService.getPolygons(List.of(FEATURE))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(orphanLineEsriJson));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(orphanLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(orphanLineEsriJson)));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);

    operatorResultProcessingService.processOutputPolygon(List.of(FEATURE), outputPolygonEsriJson, 1);

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .extracting(
            Line::getEsriJson,
            Line::getAttributes,
            Line::getNavigationType
        )
        .containsExactly(
            orphanLineEsriJson,
            Collections.emptyMap(),
            LineNavigationType.LOXODROME
        );
  }

  @Test
  void processOutputPolygon_assertParentFeaturePropertiesAreCopiedWhenSingleInputPolygon() {
    var polygonAttributes = Map.<String, Object>of("source", "parent line");
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(polygonAttributes)
        .build();
    var outputPolygonEsriJson = "output polygon";
    var outputLineEsriJson = "output line";
    var parentLineId = UUID.randomUUID();
    var parentLine = LineTestUtil.newBuilder()
        .withId(parentLineId)
        .withPolygon(polygon)
        .build();

    when(polygonService.getPolygons(List.of(FEATURE))).thenReturn(List.of(polygon));
    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);
    when(grpcClientService.calculateArea(eq(FEATURE.getCoordinateSystem()), any())).thenReturn(BigDecimal.TEN);

    operatorResultProcessingService.processOutputPolygon(List.of(FEATURE), outputPolygonEsriJson, 1);

    verify(featureService).saveFeature(featureCaptor.capture());
    var savedFeature = featureCaptor.getValue();
    var expectedFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Test Feature_1")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.TEN)
        .withStartDate(null)
        .withEndDate(null)
        .build();
    assertThat(savedFeature).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(expectedFeature);

    verify(polygonService).savePolygon(polygonCaptor.capture());
    var savedPolygon = polygonCaptor.getValue();
    var expectedPolygon = PolygonTestUtil.newBuilder()
        .withFeature(savedFeature)
        .withAttributes(polygonAttributes)
        .withStartDepth(polygon.getStartDepth())
        .withEndDepth(polygon.getEndDepth())
        .build();
    assertThat(savedPolygon).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(expectedPolygon);

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .extracting(Line::getPolygon)
        .isEqualTo(savedPolygon);
  }

  @Test
  void processOutputPolygon_assertParentFeaturePropertiesAreNotCopiedWhenMultipleInputPolygons() {
    var polygonAttributes = Map.<String, Object>of("source", "parent line");
    var polygon1 = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(polygonAttributes)
        .build();
    var polygon2 = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(polygonAttributes)
        .build();
    var outputPolygonEsriJson = "output polygon";
    var outputLineEsriJson = "output line";
    var parentLineId = UUID.randomUUID();
    var parentLine = LineTestUtil.newBuilder()
        .withId(parentLineId)
        .withPolygon(polygon1)
        .build();

    when(polygonService.getPolygons(List.of(FEATURE))).thenReturn(List.of(polygon1, polygon2));
    when(lineService.getLines(List.of(polygon1, polygon2))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);
    when(grpcClientService.calculateArea(eq(FEATURE.getCoordinateSystem()), any())).thenReturn(BigDecimal.TEN);

    operatorResultProcessingService.processOutputPolygon(List.of(FEATURE), outputPolygonEsriJson, 1);

    verify(featureService).saveFeature(featureCaptor.capture());
    var savedFeature = featureCaptor.getValue();
    var expectedFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Test Feature_1")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.TEN)
        .withStartDate(null)
        .withEndDate(null)
        .build();
    assertThat(savedFeature).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(expectedFeature);

    verify(polygonService).savePolygon(polygonCaptor.capture());
    var savedPolygon = polygonCaptor.getValue();
    var expectedPolygon = PolygonTestUtil.newBuilder()
        .withFeature(savedFeature)
        .withAttributes(new HashMap<>())
        .withStartDepth(null)
        .withEndDepth(null)
        .build();
    assertThat(savedPolygon).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(expectedPolygon);

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .singleElement()
        .extracting(Line::getPolygon)
        .isEqualTo(savedPolygon);
  }

  @Test
  void processOutputPolygon_whenMulitpleInputFeatures_verifyFeatureName() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var polygonAttributes = Map.<String, Object>of("source", "parent line");
    var polygon1 = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(polygonAttributes)
        .build();
    var outputPolygonEsriJson = "output polygon";
    var outputLineEsriJson = "output line";
    var parentLineId = UUID.randomUUID();
    var parentLine = LineTestUtil.newBuilder()
        .withId(parentLineId)
        .withPolygon(polygon1)
        .build();

    when(polygonService.getPolygons(List.of(FEATURE, feature2))).thenReturn(List.of(polygon1));
    when(lineService.getLines(List.of(polygon1))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);
    when(grpcClientService.calculateArea(eq(FEATURE.getCoordinateSystem()), any())).thenReturn(BigDecimal.TEN);

    operatorResultProcessingService.processOutputPolygon(List.of(FEATURE, feature2), outputPolygonEsriJson, 1);

    verify(featureService).saveFeature(featureCaptor.capture());
    var savedFeature = featureCaptor.getValue();
    var expectedFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("mergeResult_1")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.TEN)
        .withStartDate(null)
        .withEndDate(null)
        .build();
    assertThat(savedFeature).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(expectedFeature);
  }

  @Test
  void numberLines_whenLinesFormSingleRing_numbersLinesFromNorthwestMostLine() {
    var line1 = getUnnumberedLine("line 1"); //northwest-most line
    var line2 = getUnnumberedLine("line 2");
    var line3 = getUnnumberedLine("line 3");
    var unorderedLines = List.of(line1, line2, line3);

    when(grpcClientService.getLineStartAndEndPoints(unorderedLines)).thenReturn(List.of(
        new LineWithStartEndPoints(line2, new Point(1, 0), new Point(1, 1)),
        new LineWithStartEndPoints(line3, new Point(1, 1), new Point(0, 0)),
        new LineWithStartEndPoints(line1, new Point(0, 0), new Point(1, 0))
    ));
    when(grpcClientService.findNorthwestMostLine(any())).thenAnswer(invocation ->
        findIdForLineWithMatchingEsriJson(invocation.getArgument(0), line1.getEsriJson())
    );

    operatorResultProcessingService.numberLines(unorderedLines);

    assertThat(unorderedLines)
        .extracting(Line::getEsriJson, Line::getRingNumber, Line::getRingConnectionOrder)
        .containsExactlyInAnyOrder(
            tuple(line1.getEsriJson(), 0, 1),
            tuple(line2.getEsriJson(), 0, 2),
            tuple(line3.getEsriJson(), 0, 3)
        );
  }

  @Test
  void numberLines_whenLinesFormMultipleRings_numbersEachRingSeparately() {
    var outerRingLine1 = getUnnumberedLine("outer ring line 1");
    var outerRingLine2 = getUnnumberedLine("outer ring line 2");
    var innerRingLine1 = getUnnumberedLine("inner ring line 1");
    var innerRingLine2 = getUnnumberedLine("inner ring line 2");
    var unorderedLines = List.of(outerRingLine1, outerRingLine2, innerRingLine1, innerRingLine2);

    when(grpcClientService.getLineStartAndEndPoints(unorderedLines)).thenReturn(List.of(
        new LineWithStartEndPoints(outerRingLine1, new Point(0, 0), new Point(1, 0)),
        new LineWithStartEndPoints(outerRingLine2, new Point(1, 0), new Point(0, 0)),
        new LineWithStartEndPoints(innerRingLine1, new Point(2, 2), new Point(3, 2)),
        new LineWithStartEndPoints(innerRingLine2, new Point(3, 2), new Point(2, 2))
    ));
    when(grpcClientService.findNorthwestMostLine(any())).thenAnswer(invocation ->
        findIdForLineWithRingConnectionOrder(invocation.getArgument(0), 1)
    );

    operatorResultProcessingService.numberLines(unorderedLines);

    assertThat(unorderedLines)
        .extracting(Line::getEsriJson, Line::getRingNumber, Line::getRingConnectionOrder)
        .containsExactlyInAnyOrder(
            tuple(outerRingLine1.getEsriJson(), 0, 1),
            tuple(outerRingLine2.getEsriJson(), 0, 2),
            tuple(innerRingLine1.getEsriJson(), 1, 1),
            tuple(innerRingLine2.getEsriJson(), 1, 2)
        );
  }

  private Line getUnnumberedLine(String esriJson) {
    return LineTestUtil.newBuilder()
        .withEsriJson(esriJson)
        .withRingNumber(null)
        .withRingConnectionOrder(null)
        .build();
  }

  private UUID findIdForLineWithMatchingEsriJson(Map<UUID, Line> idToLine,
                                                 String targetEsriJson) {
    return idToLine.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getEsriJson().equals(targetEsriJson))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow();
  }

  private UUID findIdForLineWithRingConnectionOrder(Map<UUID, Line> idToLine,
                                                    int ringConnectionOrder) {
    return idToLine.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getRingConnectionOrder() == ringConnectionOrder)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow();
  }

  @Test
  void validateLinesAreValid_whenLinesCanReconstructOutputPolygon_doesNotThrowException() {
    var line = LineTestUtil.newBuilder().build();
    var outputPolygonEsriJson = "output polygon";
    var lines = List.of(line);

    when(grpcClientService.validatePolygonReconstructionFromPolylines(lines, outputPolygonEsriJson))
        .thenReturn(true);

    operatorResultProcessingService.validateLinesAreValid(lines, outputPolygonEsriJson);

    verify(grpcClientService).validatePolygonReconstructionFromPolylines(lines, outputPolygonEsriJson);
  }

  @Test
  void validateLinesAreValid_whenLinesCannotReconstructOutputPolygon_throwsException() {
    var line = LineTestUtil.newBuilder().build();
    var outputPolygonEsriJson = "output polygon";
    var lines = List.of(line);

    when(grpcClientService.validatePolygonReconstructionFromPolylines(lines, outputPolygonEsriJson))
        .thenReturn(false);

    assertThatThrownBy(() -> operatorResultProcessingService.validateLinesAreValid(lines, outputPolygonEsriJson))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Cannot generate valid polygon from processed lines for output polygon with EsriJSON: %s",
            outputPolygonEsriJson
        );

    verify(grpcClientService).validatePolygonReconstructionFromPolylines(lines, outputPolygonEsriJson);
  }
}
