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

    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);

    operatorResultProcessingService.processOutputPolygon(
        FEATURE, new EsriJsonPolygonWithSources(outputPolygonEsriJson, List.of(polygon)), 1);

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

    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(orphanLineEsriJson));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(orphanLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(orphanLineEsriJson)));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);

    operatorResultProcessingService.processOutputPolygon(
        FEATURE, new EsriJsonPolygonWithSources(outputPolygonEsriJson, List.of(polygon)), 1);

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

    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(inputLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(orphanLineEsriJson));
    when(grpcClientService.findParentLines(List.of(inputLine), List.of(orphanLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(orphanLineEsriJson)));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(any(), any())).thenReturn(true);

    operatorResultProcessingService.processOutputPolygon(
        FEATURE, new EsriJsonPolygonWithSources(outputPolygonEsriJson, List.of(polygon)), 1);

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
  void processOutputPolygon_whenSingleInputPolygon_thenCopiesPolygonAttributesAndDepths() {
    var polygonAttributes = Map.<String, Object>of("source", "parent polygon");
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(polygonAttributes)
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();
    var outputPolygonEsriJson = "output polygon";
    var outputLineEsriJson = "output line";
    var parentLineId = UUID.randomUUID();
    var parentLine = LineTestUtil.newBuilder()
        .withId(parentLineId)
        .withPolygon(polygon)
        .build();

    when(lineService.getLines(List.of(polygon))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon(outputPolygonEsriJson)).thenReturn(List.of(outputLineEsriJson));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of(outputLineEsriJson)))
        .thenReturn(new FindParentLineResponse(Map.of(outputLineEsriJson, parentLineId), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq(outputPolygonEsriJson)))
        .thenReturn(true);
    when(grpcClientService.calculateArea(eq(FEATURE.getCoordinateSystem()), anyList())).thenReturn(BigDecimal.TEN);

    operatorResultProcessingService.processOutputPolygon(
        FEATURE, new EsriJsonPolygonWithSources(outputPolygonEsriJson, List.of(polygon)), 1);

    var savedFeature = captureSavedFeature();
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

    var savedPolygons = captureSavedPolygons();
    assertThat(savedPolygons).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedPolygon(savedFeature, polygonAttributes, -100L, -200L)));

    assertThat(captureSavedLines()).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedLine(
            savedPolygons.getFirst(),
            outputLineEsriJson,
            parentLine.getAttributes(),
            parentLine.getNavigationType()
        )));
  }

  @Test
  void processOutputPolygons_whenSingleSource_thenCopiesPolygonAndLineAttributesAndDepths() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var polygonAttributes = Map.<String, Object>of("source", "parent polygon");
    var sourcePolygon = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(polygonAttributes)
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();
    var parentLine = LineTestUtil.newBuilder()
        .withPolygon(sourcePolygon)
        .withAttributes(Map.of("line", "parent"))
        .withNavigationType(LineNavigationType.LOXODROME)
        .build();

    when(lineService.getLines(List.of(sourcePolygon))).thenReturn(List.of(parentLine));
    when(grpcClientService.explodePolygon("output polygon")).thenReturn(List.of("output line"));
    when(grpcClientService.findParentLines(List.of(parentLine), List.of("output line")))
        .thenReturn(new FindParentLineResponse(Map.of("output line", parentLine.getId()), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq("output polygon")))
        .thenReturn(true);

    operatorResultProcessingService.processOutputPolygons(
        List.of(FEATURE, feature2),
        List.of(new EsriJsonPolygonWithSources("output polygon", List.of(sourcePolygon)))
    );

    var savedFeature = captureSavedFeature();
    var savedPolygons = captureSavedPolygons();
    assertThat(savedPolygons).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedPolygon(savedFeature, polygonAttributes, -100L, -200L)));
    assertThat(captureSavedLines()).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedLine(
            savedPolygons.getFirst(),
            "output line",
            Map.of("line", "parent"),
            LineNavigationType.LOXODROME
        )));
  }

  @Test
  void processOutputPolygons_whenSourcesAtDifferentDepths_thenSpansDepthsAndClearsAttributesButKeepsNavigationType() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var upperPolygon = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(Map.of("source", "upper"))
        .withStartDepth(0L)
        .withEndDepth(-100L)
        .build();
    var lowerPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature2)
        .withAttributes(Map.of("source", "lower"))
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();
    var upperLine = LineTestUtil.newBuilder()
        .withPolygon(upperPolygon)
        .withAttributes(Map.of("line", "upper"))
        .withNavigationType(LineNavigationType.CARTESIAN)
        .build();
    var lowerLine = LineTestUtil.newBuilder()
        .withPolygon(lowerPolygon)
        .withAttributes(Map.of("line", "lower"))
        .withNavigationType(LineNavigationType.GEODESIC)
        .build();
    var outputLines = List.of("output line 1", "output line 2");
    var outputLineToParentLineId = new LinkedHashMap<String, UUID>();
    outputLineToParentLineId.put("output line 1", upperLine.getId());
    outputLineToParentLineId.put("output line 2", lowerLine.getId());

    when(lineService.getLines(List.of(lowerPolygon, upperPolygon))).thenReturn(List.of(upperLine, lowerLine));
    when(grpcClientService.explodePolygon("output polygon")).thenReturn(outputLines);
    when(grpcClientService.findParentLines(List.of(lowerLine, upperLine), outputLines))
        .thenReturn(new FindParentLineResponse(outputLineToParentLineId, List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq("output polygon")))
        .thenReturn(true);

    operatorResultProcessingService.processOutputPolygons(
        List.of(FEATURE, feature2),
        List.of(new EsriJsonPolygonWithSources("output polygon", List.of(lowerPolygon, upperPolygon)))
    );

    var savedFeature = captureSavedFeature();
    var savedPolygons = captureSavedPolygons();
    assertThat(savedPolygons).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedPolygon(savedFeature, Map.of(), 0L, -200L)));
    assertThat(captureSavedLines()).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(
            expectedLine(savedPolygons.getFirst(), "output line 1", Map.of(), LineNavigationType.CARTESIAN),
            expectedLine(savedPolygons.getFirst(), "output line 2", Map.of(), LineNavigationType.GEODESIC)
        ));
  }

  @Test
  void processOutputPolygons_whenSourcesAtSameDepths_thenClearsPolygonAttributesButKeepsLineAttributes() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var leftPolygon = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(Map.of("source", "left"))
        .withStartDepth(0L)
        .withEndDepth(-100L)
        .build();
    var rightPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature2)
        .withAttributes(Map.of("source", "right"))
        .withStartDepth(0L)
        .withEndDepth(-100L)
        .build();
    var leftLine = LineTestUtil.newBuilder()
        .withPolygon(leftPolygon)
        .withAttributes(Map.of("line", "left"))
        .withNavigationType(LineNavigationType.LOXODROME)
        .build();
    var rightLine = LineTestUtil.newBuilder()
        .withPolygon(rightPolygon)
        .withAttributes(Map.of("line", "right"))
        .withNavigationType(LineNavigationType.LOXODROME)
        .build();
    var outputLines = List.of("output line 1", "output line 2");
    var outputLineToParentLineId = new LinkedHashMap<String, UUID>();
    outputLineToParentLineId.put("output line 1", leftLine.getId());
    outputLineToParentLineId.put("output line 2", rightLine.getId());

    when(lineService.getLines(List.of(leftPolygon, rightPolygon))).thenReturn(List.of(leftLine, rightLine));
    when(grpcClientService.explodePolygon("output polygon")).thenReturn(outputLines);
    when(grpcClientService.findParentLines(List.of(leftLine, rightLine), outputLines))
        .thenReturn(new FindParentLineResponse(outputLineToParentLineId, List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq("output polygon")))
        .thenReturn(true);

    operatorResultProcessingService.processOutputPolygons(
        List.of(FEATURE, feature2),
        List.of(new EsriJsonPolygonWithSources("output polygon", List.of(leftPolygon, rightPolygon)))
    );

    var savedFeature = captureSavedFeature();
    var savedPolygons = captureSavedPolygons();
    assertThat(savedPolygons).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedPolygon(savedFeature, Map.of(), 0L, -100L)));
    assertThat(captureSavedLines()).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(
            expectedLine(savedPolygons.getFirst(), "output line 1", Map.of("line", "left"), LineNavigationType.LOXODROME),
            expectedLine(savedPolygons.getFirst(), "output line 2", Map.of("line", "right"), LineNavigationType.LOXODROME)
        ));
  }

  @Test
  void processOutputPolygons_whenSourceDepthsAreNull_thenNewPolygonDepthsStayNull() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var surfacePolygon = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withStartDepth(null)
        .withEndDepth(-100L)
        .build();
    var bottomlessPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature2)
        .withStartDepth(-100L)
        .withEndDepth(null)
        .build();
    var surfaceLine = LineTestUtil.newBuilder().withPolygon(surfacePolygon).build();
    var bottomlessLine = LineTestUtil.newBuilder().withPolygon(bottomlessPolygon).build();

    when(lineService.getLines(List.of(surfacePolygon, bottomlessPolygon)))
        .thenReturn(List.of(surfaceLine, bottomlessLine));
    when(grpcClientService.explodePolygon("output polygon")).thenReturn(List.of("output line"));
    when(grpcClientService.findParentLines(List.of(surfaceLine, bottomlessLine), List.of("output line")))
        .thenReturn(new FindParentLineResponse(Map.of("output line", surfaceLine.getId()), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq("output polygon")))
        .thenReturn(true);

    operatorResultProcessingService.processOutputPolygons(
        List.of(FEATURE, feature2),
        List.of(new EsriJsonPolygonWithSources("output polygon", List.of(surfacePolygon, bottomlessPolygon)))
    );

    var savedFeature = captureSavedFeature();
    assertThat(captureSavedPolygons()).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(expectedPolygon(savedFeature, Map.of(), null, null)));
  }

  @Test
  void processOutputPolygons_whenMultipleOutputs_thenMatchesEachOutputOnlyToItsOwnSourcesParentLines() {
    var feature2 = FeatureTestUtil.newBuilder().build();
    var sourcePolygon1 = PolygonTestUtil.newBuilder()
        .withFeature(FEATURE)
        .withAttributes(Map.of("source", "1"))
        .build();
    var sourcePolygon2 = PolygonTestUtil.newBuilder()
        .withFeature(feature2)
        .withAttributes(Map.of("source", "2"))
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();
    var parentLine1 = LineTestUtil.newBuilder()
        .withPolygon(sourcePolygon1)
        .withAttributes(Map.of("line", "1"))
        .withNavigationType(LineNavigationType.CARTESIAN)
        .build();
    var parentLine2 = LineTestUtil.newBuilder()
        .withPolygon(sourcePolygon2)
        .withAttributes(Map.of("line", "2"))
        .withNavigationType(LineNavigationType.LOXODROME)
        .build();

    when(lineService.getLines(List.of(sourcePolygon1, sourcePolygon2))).thenReturn(List.of(parentLine1, parentLine2));
    when(grpcClientService.explodePolygon("output polygon 1")).thenReturn(List.of("output line 1"));
    when(grpcClientService.explodePolygon("output polygon 2")).thenReturn(List.of("output line 2"));
    when(grpcClientService.findParentLines(List.of(parentLine1), List.of("output line 1")))
        .thenReturn(new FindParentLineResponse(Map.of("output line 1", parentLine1.getId()), List.of()));
    when(grpcClientService.findParentLines(List.of(parentLine2), List.of("output line 2")))
        .thenReturn(new FindParentLineResponse(Map.of("output line 2", parentLine2.getId()), List.of()));
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq("output polygon 1")))
        .thenReturn(true);
    when(grpcClientService.validatePolygonReconstructionFromPolylines(anyList(), eq("output polygon 2")))
        .thenReturn(true);
    when(grpcClientService.calculateArea(eq(FEATURE.getCoordinateSystem()), anyList())).thenReturn(BigDecimal.TEN);

    var result = operatorResultProcessingService.processOutputPolygons(
        List.of(FEATURE, feature2),
        List.of(
            new EsriJsonPolygonWithSources("output polygon 1", List.of(sourcePolygon1)),
            new EsriJsonPolygonWithSources("output polygon 2", List.of(sourcePolygon2))
        )
    );

    var savedFeature = captureSavedFeature();
    var expectedFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.TEN)
        .withStartDate(null)
        .withEndDate(null)
        .build();
    assertThat(savedFeature).isSameAs(result);
    assertThat(savedFeature).usingRecursiveComparison()
        .ignoringFields("id", "legacyId", "featureName")
        .isEqualTo(expectedFeature);
    assertThat(savedFeature.getFeatureName()).matches("mergeResult_[0-9A-F]{4}");

    var savedPolygons = captureSavedPolygons();
    assertThat(savedPolygons).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(
            expectedPolygon(savedFeature, Map.of("source", "1"), 0L, -100L),
            expectedPolygon(savedFeature, Map.of("source", "2"), -100L, -200L)
        ));
    assertThat(captureSavedLines()).usingRecursiveComparison()
        .ignoringFields("id", "legacyId")
        .isEqualTo(List.of(
            expectedLine(savedPolygons.get(0), "output line 1", Map.of("line", "1"), LineNavigationType.CARTESIAN),
            expectedLine(savedPolygons.get(1), "output line 2", Map.of("line", "2"), LineNavigationType.LOXODROME)
        ));
  }

  private Feature captureSavedFeature() {
    verify(featureService).saveFeature(featureCaptor.capture());
    return featureCaptor.getValue();
  }

  private List<Polygon> captureSavedPolygons() {
    verify(polygonService).savePolygons(polygonsCaptor.capture());
    return List.copyOf(polygonsCaptor.getValue());
  }

  private List<Line> captureSavedLines() {
    verify(lineService).saveLines(linesCaptor.capture());
    return linesCaptor.getValue();
  }

  private static Polygon expectedPolygon(
      Feature feature,
      Map<String, Object> attributes,
      Long startDepth,
      Long endDepth
  ) {
    return PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withAttributes(attributes)
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();
  }

  private static Line expectedLine(
      Polygon polygon,
      String esriJson,
      Map<String, Object> attributes,
      LineNavigationType navigationType
  ) {
    return LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withEsriJson(esriJson)
        .withAttributes(attributes)
        .withNavigationType(navigationType)
        .withRingNumber(null)
        .withDisplayOrder(null)
        .build();
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
        argThat(polygons -> polygons.size() == 2
            && polygons.get(0).size() == 1 && "kept line".equals(polygons.get(0).getFirst().getEsriJson())
            && polygons.get(1).size() == 1 && "cropped line".equals(polygons.get(1).getFirst().getEsriJson()))
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
        .withFeatureArea(BigDecimal.TEN)
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
  void processStrataSplitFeature_savesNewFeatureWithGivenPolygonsAndCopiesOfTheirSourceLines() {
    var parentFeature = FeatureTestUtil.newBuilder().build();
    var inputFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Subarea A")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withParentFeature(parentFeature)
        .withStartDate(START_DATE)
        .build();
    var sourcePolygon = PolygonTestUtil.newBuilder().withFeature(inputFeature).build();
    var sourceLine = LineTestUtil.newBuilder()
        .withPolygon(sourcePolygon)
        .withEsriJson("source line")
        .withAttributes(Map.of("line", "source"))
        .withNavigationType(LineNavigationType.LOXODROME)
        .withRingNumber(5)
        .withDisplayOrder(5)
        .build();
    var newPolygon = new Polygon();
    newPolygon.setAttributes(Map.of("polygon", "new"));
    newPolygon.setStartDepth(0L);
    newPolygon.setEndDepth(-500L);

    when(grpcClientService.getLineStartAndEndPoints(
        argThat(lines -> lines.size() == 1 && "source line".equals(lines.getFirst().getEsriJson())),
        eq(false)
    ))
        .thenAnswer(invocation -> {
          List<Line> lines = invocation.getArgument(0);
          return List.of(new LineWithStartEndPoints(lines.getFirst(), new Point(0, 0), new Point(0, 0)));
        });
    when(grpcClientService.calculateArea(
        eq(CoordinateSystem.ED50),
        argThat(polygons -> polygons.size() == 1
            && polygons.getFirst().size() == 1 && "source line".equals(polygons.getFirst().getFirst().getEsriJson()))
    ))
        .thenReturn(BigDecimal.TEN);

    var result = operatorResultProcessingService.processStrataSplitFeature(
        new EntityBackedFeature(inputFeature, Map.of(sourcePolygon, List.of(sourceLine))),
        Map.of(newPolygon, sourcePolygon),
        "above"
    );

    var expectedFeature = FeatureTestUtil.newBuilder()
        .withFeatureName("Subarea A_above")
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withAttributes(FEATURE_ATTRIBUTES)
        .withFeatureArea(BigDecimal.TEN)
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
    assertThat(polygonsCaptor.getValue()).containsExactly(newPolygon);
    assertThat(newPolygon.getFeature()).isSameAs(result);

    verify(lineService).saveLines(linesCaptor.capture());
    assertThat(linesCaptor.getValue())
        .extracting(
            Line::getPolygon,
            Line::getEsriJson,
            Line::getAttributes,
            Line::getNavigationType,
            Line::getRingNumber,
            Line::getDisplayOrder
        )
        .containsExactly(
            tuple(newPolygon, "source line", Map.of("line", "source"), LineNavigationType.LOXODROME, 0, 1)
        );
    assertThat(linesCaptor.getValue().getFirst()).isNotSameAs(sourceLine);
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
