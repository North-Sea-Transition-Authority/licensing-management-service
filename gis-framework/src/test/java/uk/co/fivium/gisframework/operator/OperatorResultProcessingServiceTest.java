package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esri.core.geometry.Point;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

  @Captor
  private ArgumentCaptor<Collection<Polygon>> polygonsCaptor;

  @InjectMocks
  private OperatorResultProcessingService operatorResultProcessingService;

  private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
  private static final Map<String, String> FEATURE_ATTRIBUTES = Map.of("KEY", "VALUE");
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
        .withFeatureName("mergeResult_[0-9A-F]{4}")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.TEN)
        .withStartDate(null)
        .withEndDate(null)
        .build();
    assertThat(savedFeature).usingRecursiveComparison()
        .ignoringFields("id", "legacyId", "featureName")
        .isEqualTo(expectedFeature);
    assertThat(savedFeature.getFeatureName()).matches("mergeResult_[0-9A-F]{4}");
  }

  @Test
  void processOutputPolygons_whenMultipleOutputParts_buildsAndSavesOnePolygonPerPart() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var inputPolygon = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var inputLine = LineTestUtil.newBuilder()
        .withPolygon(inputPolygon)
        .withNavigationType(LineNavigationType.CARTESIAN)
        .build();
    var outputPart1 = "output part 1";
    var outputPart2 = "output part 2";
    var outputLine1 = "output line 1";
    var outputLine2 = "output line 2";

    when(polygonService.getPolygons(List.of(FEATURE, feature2))).thenReturn(List.of(inputPolygon));
    when(lineService.getLines(List.of(inputPolygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPart1)).thenReturn(List.of(outputLine1));
    when(grpcClientService.explodePolygon(outputPart2)).thenReturn(List.of(outputLine2));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(outputLine1)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(outputLine1)));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(outputLine2)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(outputLine2)));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), anyString())).thenReturn(true);
    when(grpcClientService.calculateArea(eq(FEATURE.getCoordinateSystem()), anyList())).thenReturn(BigDecimal.TEN);

    operatorResultProcessingService.processOutputPolygons(List.of(FEATURE, feature2), List.of(outputPart1, outputPart2), 1);

    verify(featureService).saveFeature(featureCaptor.capture());
    assertThat(featureCaptor.getValue().getFeatureName()).matches("mergeResult_[0-9A-F]{4}");

    verify(polygonService, times(2)).savePolygon(polygonCaptor.capture());
    var savedPolygons = polygonCaptor.getAllValues();
    assertThat(savedPolygons).hasSize(2);

    verify(lineService).saveLines(linesCaptor.capture());
    var savedLines = linesCaptor.getValue();
    assertThat(savedLines)
        .extracting(Line::getEsriJson, Line::getPolygon)
        .containsExactly(
            tuple(outputLine1, savedPolygons.get(0)),
            tuple(outputLine2, savedPolygons.get(1))
        );
  }

  @Test
  void validateLinesAreValidForParts_whenEachPolygonReconstructsFromAPart_matchesEachPartOnce() {
    var polygon1 = PolygonTestUtil.newBuilder().build();
    var polygon2 = PolygonTestUtil.newBuilder().build();
    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).build();
    var part1 = "part 1";
    var part2 = "part 2";

    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), anyString())).thenReturn(true);

    operatorResultProcessingService.validateLinesAreValid(List.of(line1, line2), List.of(part1, part2));

    verify(grpcClientService, times(2)).validatePolygonReconstructionFromPolylines(anyList(), anyString());
  }

  @Test
  void validateLinesAreValidForParts_whenAPolygonCannotBeReconstructedFromAnyPart_throwsException() {
    var polygon = PolygonTestUtil.newBuilder().build();
    var line = LineTestUtil.newBuilder().withPolygon(polygon).withEsriJson("line json").build();
    var lines = List.of(line);
    var parts = List.of("part 1");

    when(grpcClientService.validatePolygonReconstructionFromPolylines(lines, "part 1")).thenReturn(false);

    assertThatThrownBy(() -> operatorResultProcessingService.validateLinesAreValid(lines, parts))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Cannot generate valid polygon from processed lines with EsriJSON: %s",
            List.of("line json")
        );
  }

  @Test
  void numberLines_whenLinesFormSingleRing_numbersLinesFromTopMostThenWestMostLine() {
    var polygon = PolygonTestUtil.newBuilder().build();
    var line1 = getUnnumberedLine("line 1", polygon);
    var line2 = getUnnumberedLine("line 2", polygon);
    var line3 = getUnnumberedLine("line 3", polygon); //top-most start point, so numbering starts here
    var unorderedLines = List.of(line1, line2, line3);

    when(grpcClientService.getLineStartAndEndPoints(unorderedLines, false)).thenReturn(List.of(
        new LineWithStartEndPoints(line2, new Point(1, 0), new Point(1, 1)),
        new LineWithStartEndPoints(line3, new Point(1, 1), new Point(0, 0)),
        new LineWithStartEndPoints(line1, new Point(0, 0), new Point(1, 0))
    ));

    operatorResultProcessingService.numberLines(unorderedLines);

    assertThat(unorderedLines)
        .extracting(Line::getEsriJson, Line::getRingNumber, Line::getDisplayOrder)
        .containsExactlyInAnyOrder(
            tuple(line3.getEsriJson(), 0, 1),
            tuple(line1.getEsriJson(), 0, 2),
            tuple(line2.getEsriJson(), 0, 3)
        );
  }

  @Test
  void numberLines_whenLinesFormMultipleRings_continuesConnectionOrderAcrossRings() {
    var polygon = PolygonTestUtil.newBuilder().build();
    var outerRingLine1 = getUnnumberedLine("outer ring line 1", polygon);
    var outerRingLine2 = getUnnumberedLine("outer ring line 2", polygon);
    var innerRingLine1 = getUnnumberedLine("inner ring line 1", polygon);
    var innerRingLine2 = getUnnumberedLine("inner ring line 2", polygon);
    var unorderedLines = List.of(outerRingLine1, outerRingLine2, innerRingLine1, innerRingLine2);

    // The inner ring has the top-most start point, so it is numbered first (ring 0). The outer ring's
    // connection order continues on from the inner ring rather than restarting at 1.
    when(grpcClientService.getLineStartAndEndPoints(unorderedLines, false)).thenReturn(List.of(
        new LineWithStartEndPoints(outerRingLine1, new Point(0, 0), new Point(1, 0)),
        new LineWithStartEndPoints(outerRingLine2, new Point(1, 0), new Point(0, 0)),
        new LineWithStartEndPoints(innerRingLine1, new Point(2, 2), new Point(3, 2)),
        new LineWithStartEndPoints(innerRingLine2, new Point(3, 2), new Point(2, 2))
    ));

    operatorResultProcessingService.numberLines(unorderedLines);

    assertThat(unorderedLines)
        .extracting(Line::getEsriJson, Line::getRingNumber, Line::getDisplayOrder)
        .containsExactlyInAnyOrder(
            tuple(innerRingLine1.getEsriJson(), 0, 1),
            tuple(innerRingLine2.getEsriJson(), 0, 2),
            tuple(outerRingLine1.getEsriJson(), 1, 3),
            tuple(outerRingLine2.getEsriJson(), 1, 4)
        );
  }

  @Test
  void numberLines_whenLinesSpanMultiplePolygons_thenNumbersTopMostPolygonFirstWithContinuousOrder() {
    var topPolygon = PolygonTestUtil.newBuilder().build();
    var bottomPolygon = PolygonTestUtil.newBuilder().build();

    var topOuterLine1 = getUnnumberedLine("top outer line 1", topPolygon);
    var topOuterLine2 = getUnnumberedLine("top outer line 2", topPolygon);
    var topOuterLine3 = getUnnumberedLine("top outer line 3", topPolygon);
    var topInnerLine1 = getUnnumberedLine("top inner line 1", topPolygon);
    var topInnerLine2 = getUnnumberedLine("top inner line 2", topPolygon);
    var topInnerLine3 = getUnnumberedLine("top inner line 3", topPolygon);

    var bottomLine1 = getUnnumberedLine("bottom line 1", bottomPolygon);
    var bottomLine2 = getUnnumberedLine("bottom line 2", bottomPolygon);
    var bottomLine3 = getUnnumberedLine("bottom line 3", bottomPolygon);
    var unorderedLines = List.of(
        bottomLine1, topInnerLine1, topOuterLine1, bottomLine2, topInnerLine2,
        topOuterLine2, bottomLine3, topInnerLine3, topOuterLine3
    );

    // topPolygon's outer ring holds the top-most start point so it is numbered first, followed by its
    // inner ring, then bottomPolygon. The inner ring's top-most point sits below bottomPolygon's, so a
    // global (non-polygon-aware) sort would interleave them; grouping by polygon must not.
    // ringNumber and ringConnectionOrder continue across both polygons.
    when(grpcClientService.getLineStartAndEndPoints(unorderedLines, false)).thenReturn(List.of(
        new LineWithStartEndPoints(topOuterLine1, new Point(0, 10), new Point(8, 10)),
        new LineWithStartEndPoints(topOuterLine2, new Point(8, 10), new Point(0, 0)),
        new LineWithStartEndPoints(topOuterLine3, new Point(0, 0), new Point(0, 10)),
        new LineWithStartEndPoints(topInnerLine1, new Point(1, 3), new Point(2, 3)),
        new LineWithStartEndPoints(topInnerLine2, new Point(2, 3), new Point(1, 1)),
        new LineWithStartEndPoints(topInnerLine3, new Point(1, 1), new Point(1, 3)),
        new LineWithStartEndPoints(bottomLine1, new Point(10, 5), new Point(14, 5)),
        new LineWithStartEndPoints(bottomLine2, new Point(14, 5), new Point(10, 0)),
        new LineWithStartEndPoints(bottomLine3, new Point(10, 0), new Point(10, 5))
    ));

    operatorResultProcessingService.numberLines(unorderedLines);

    assertThat(unorderedLines)
        .extracting(Line::getEsriJson, Line::getRingNumber, Line::getDisplayOrder)
        .containsExactlyInAnyOrder(
            tuple(topOuterLine1.getEsriJson(), 0, 1),
            tuple(topOuterLine2.getEsriJson(), 0, 2),
            tuple(topOuterLine3.getEsriJson(), 0, 3),
            tuple(topInnerLine1.getEsriJson(), 1, 4),
            tuple(topInnerLine2.getEsriJson(), 1, 5),
            tuple(topInnerLine3.getEsriJson(), 1, 6),
            tuple(bottomLine1.getEsriJson(), 2, 7),
            tuple(bottomLine2.getEsriJson(), 2, 8),
            tuple(bottomLine3.getEsriJson(), 2, 9)
        );
  }

  private Line getUnnumberedLine(String esriJson, Polygon polygon) {
    return LineTestUtil.newBuilder()
        .withEsriJson(esriJson)
        .withPolygon(polygon)
        .withRingNumber(null)
        .withDisplayOrder(null)
        .build();
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
            "Cannot generate valid polygon from processed lines with EsriJSON: %s"
                .formatted(lines.stream().map(Line::getEsriJson).toList())
        );

    verify(grpcClientService).validatePolygonReconstructionFromPolylines(lines, outputPolygonEsriJson);
  }

  @Test
  void processCroppedFeature_whenPolygonsKeptCroppedAndRemoved_savesNewFeatureWithKeptAndCroppedPolygons() {
    var parentFeature = FeatureTestUtil.newBuilder().build();
    var inputFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Subarea A")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withParentFeature(parentFeature)
        .withStartDate(START_DATE)
        .build();
    var keptPolygon = PolygonTestUtil.newBuilder()
        .withFeature(inputFeature)
        .withAttributes(Map.of("polygon", "kept"))
        .withStartDepth(1L)
        .withEndDepth(2L)
        .build();
    var croppedPolygon = PolygonTestUtil.newBuilder()
        .withFeature(inputFeature)
        .withAttributes(Map.of("polygon", "cropped"))
        .withStartDepth(3L)
        .withEndDepth(4L)
        .build();
    var removedPolygon = PolygonTestUtil.newBuilder().withFeature(inputFeature).build();
    var keptLine = LineTestUtil.newBuilder()
        .withPolygon(keptPolygon)
        .withEsriJson("kept line")
        .withAttributes(Map.of("line", "kept"))
        .withNavigationType(LineNavigationType.GEODESIC)
        .build();
    var croppedParentLineId = UUID.randomUUID();
    var croppedParentLine = LineTestUtil.newBuilder()
        .withId(croppedParentLineId)
        .withPolygon(croppedPolygon)
        .withAttributes(Map.of("line", "cropped parent"))
        .withNavigationType(LineNavigationType.LOXODROME)
        .build();
    var removedLine = LineTestUtil.newBuilder().withPolygon(removedPolygon).build();

    Map<Polygon, List<Line>> polygonToLines = new LinkedHashMap<>();
    polygonToLines.put(keptPolygon, List.of(keptLine));
    polygonToLines.put(croppedPolygon, List.of(croppedParentLine));
    polygonToLines.put(removedPolygon, List.of(removedLine));

    when(grpcClientService.explodePolygon("cropped polygon")).thenReturn(List.of("cropped line"));
    when(grpcClientService.findParentLines(List.of(croppedParentLine), List.of("cropped line")))
        .thenReturn(new FindParentLineResponse(Map.of("cropped line", croppedParentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), eq("cropped polygon"))).thenReturn(true);
    when(grpcClientService.calculateArea(
        eq(CoordinateSystem.ED50),
        argThat(lines -> lines.size() == 1 && "kept line".equals(lines.getFirst().getEsriJson()))
    ))
        .thenReturn(BigDecimal.ONE);
    when(grpcClientService.calculateArea(
        eq(CoordinateSystem.ED50),
        argThat(lines -> lines.size() == 1 && "cropped line".equals(lines.getFirst().getEsriJson()))
    ))
        .thenReturn(BigDecimal.TEN);

    var result = operatorResultProcessingService.processCroppedFeature(
        new EntityBackedFeature(inputFeature, polygonToLines),
        Map.of(croppedPolygon.getId(), "cropped polygon"),
        Set.of(removedPolygon.getId())
    );

    var expectedFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Subarea A")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.valueOf(11))
        .withParentFeature(parentFeature)
        .withStartDate(null)
        .withEndDate(null)
        .build();
    verify(featureService).saveFeature(featureCaptor.capture());
    assertThat(featureCaptor.getValue()).isSameAs(result);
    assertThat(result).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(expectedFeature);

    verify(polygonService).savePolygons(polygonsCaptor.capture());
    assertThat(polygonsCaptor.getValue())
        .extracting(Polygon::getFeature, Polygon::getAttributes, Polygon::getStartDepth, Polygon::getEndDepth)
        .containsExactly(
            tuple(result, Map.of("polygon", "kept"), 1L, 2L),
            tuple(result, Map.of("polygon", "cropped"), 3L, 4L)
        );
    var savedPolygons = List.copyOf(polygonsCaptor.getValue());

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .extracting(Line::getPolygon, Line::getEsriJson, Line::getAttributes, Line::getNavigationType)
        .containsExactly(
            tuple(savedPolygons.get(0), "kept line", Map.of("line", "kept"), LineNavigationType.GEODESIC),
            tuple(savedPolygons.get(1), "cropped line", Map.of("line", "cropped parent"), LineNavigationType.LOXODROME)
        );
  }

  @Test
  void processCroppedFeature_whenCroppedLinesCannotReconstructCroppedPolygon_throwsException() {
    var croppedPolygon = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var croppedParentLineId = UUID.randomUUID();
    var croppedParentLine = LineTestUtil.newBuilder()
        .withId(croppedParentLineId)
        .withPolygon(croppedPolygon)
        .build();
    var entityBackedFeature = new EntityBackedFeature(FEATURE, Map.of(croppedPolygon, List.of(croppedParentLine)));
    var polygonIdToCroppedEsriJson = Map.of(croppedPolygon.getId(), "cropped polygon");

    when(grpcClientService.explodePolygon("cropped polygon")).thenReturn(List.of("cropped line"));
    when(grpcClientService.findParentLines(List.of(croppedParentLine), List.of("cropped line")))
        .thenReturn(new FindParentLineResponse(Map.of("cropped line", croppedParentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), eq("cropped polygon"))).thenReturn(false);

    assertThatThrownBy(() -> operatorResultProcessingService.processCroppedFeature(
        entityBackedFeature,
        polygonIdToCroppedEsriJson,
        Set.of()
    ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Cannot generate valid polygon from processed lines with EsriJSON: [cropped line]");
  }
}
