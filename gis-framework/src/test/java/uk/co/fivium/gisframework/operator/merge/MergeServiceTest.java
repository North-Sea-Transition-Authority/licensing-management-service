package uk.co.fivium.gisframework.operator.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.FindParentLineResponse;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.operator.EsriJsonPolygonWithSources;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

  private static final String SHAPE_X = "shape x";
  private static final String SHAPE_Y = "shape y";
  private static final String SHAPE_Z = "shape z";

  private static final String EDGE = "edge";
  private static final String INTERNAL_EDGE = "internal edge";
  private static final String LEFT_PIECE = "left piece";
  private static final String RIGHT_PIECE = "right piece";
  private static final String LEFT_SEGMENT = "left segment";
  private static final String RIGHT_SEGMENT = "right segment";
  private static final Map<String, List<String>> LINE_TO_SEGMENTS_WITHIN = Map.of(
      EDGE, List.of(LEFT_SEGMENT, RIGHT_SEGMENT),
      INTERNAL_EDGE, List.of(),
      LEFT_PIECE, List.of(LEFT_SEGMENT),
      RIGHT_PIECE, List.of(RIGHT_SEGMENT)
  );

  private static final Map<String, Object> NORTH_EDGE = Map.of("DESCRIPTION", "North edge");
  private static final Map<String, Object> OTHER_EDGE = Map.of("DESCRIPTION", "Other edge");

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private LineService lineService;

  @InjectMocks
  private MergeService mergeService;

  @Test
  void mergeWithDepths_whenSameShapeAndDepthsMeet_thenStacksIntoOnePolygon() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

    when(lineService.getLines(List.of(upper, lower))).thenReturn(edges(upper, lower));
    when(grpcClientService.groupEqualPolygons(Map.of(id(upper), SHAPE_X, id(lower), SHAPE_X)))
        .thenReturn(List.of(List.of(id(upper), id(lower))));

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_X, lower)));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, upper, lower)));
  }

  @Test
  void mergeWithDepths_whenSameShapeWithDepthGap_thenPolygonsUnchanged() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-150L, -200L);

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_X, lower)));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_X, lower)));
    verifyNoInteractions(grpcClientService);
  }

  @Test
  void mergeWithDepths_whenDifferentShapesWithOverlappingDepths_thenPolygonsUnchanged() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-50L, -150L);

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_Y, lower)));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_Y, lower)));
    verifyNoInteractions(grpcClientService);
  }

  @Test
  void mergeWithDepths_whenDifferentShapesAndDepthsMeet_thenPolygonsUnchanged() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

    when(grpcClientService.groupEqualPolygons(Map.of(id(upper), SHAPE_X, id(lower), SHAPE_Y)))
        .thenReturn(List.of(List.of(id(upper)), List.of(id(lower))));

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_Y, lower)));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_Y, lower)));
  }

  @Test
  void mergeWithDepths_whenPolygonsMatchEachRule_thenOnlyStacksSameShapeWhereDepthsMeet() {
    var featureAPolygon = polygon(-100L, -200L);
    var sameShapeMeeting = polygon(0L, -100L);
    var sameShapeWithGap = polygon(-300L, -400L);
    var differentShapeOverlapping = polygon(-150L, -250L);
    var differentShapeSeparate = polygon(-450L, -500L);

    when(lineService.getLines(List.of(
        featureAPolygon,
        sameShapeMeeting,
        sameShapeWithGap,
        differentShapeOverlapping,
        differentShapeSeparate
    ))).thenReturn(edges(featureAPolygon, sameShapeMeeting));
    when(grpcClientService.groupEqualPolygons(Map.of(
        id(featureAPolygon), SHAPE_X,
        id(sameShapeMeeting), SHAPE_X,
        id(sameShapeWithGap), SHAPE_X,
        id(differentShapeOverlapping), SHAPE_Y,
        id(differentShapeSeparate), SHAPE_Z
    ))).thenReturn(List.of(
        List.of(id(featureAPolygon), id(sameShapeMeeting), id(sameShapeWithGap)),
        List.of(id(differentShapeOverlapping)),
        List.of(id(differentShapeSeparate))
    ));

    var result = mergeService.mergeWithDepths(List.of(
        withSources(SHAPE_X, featureAPolygon),
        withSources(SHAPE_X, sameShapeMeeting),
        withSources(SHAPE_X, sameShapeWithGap),
        withSources(SHAPE_Y, differentShapeOverlapping),
        withSources(SHAPE_Z, differentShapeSeparate)
    ));

    assertThat(result).isEqualTo(List.of(
        withSources(SHAPE_X, featureAPolygon, sameShapeMeeting),
        withSources(SHAPE_X, sameShapeWithGap),
        withSources(SHAPE_Y, differentShapeOverlapping),
        withSources(SHAPE_Z, differentShapeSeparate)
    ));
  }

  @Test
  void mergeWithDepths_whenSameShapeAboveAndBelow_thenStacksAllThree() {
    var middle = polygon(-100L, -200L);
    var above = polygon(0L, -100L);
    var below = polygon(-200L, -300L);

    when(lineService.getLines(List.of(middle, above, below))).thenReturn(edges(middle, above, below));
    when(grpcClientService.groupEqualPolygons(Map.of(id(middle), SHAPE_X, id(above), SHAPE_X, id(below), SHAPE_X)))
        .thenReturn(List.of(List.of(id(middle), id(above), id(below))));

    var result = mergeService.mergeWithDepths(List.of(
        withSources(SHAPE_X, middle),
        withSources(SHAPE_X, above),
        withSources(SHAPE_X, below)
    ));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, middle, above, below)));
  }

  @Test
  void mergeWithDepths_whenPolygonNotInAnyEqualGroup_thenThrows() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);
    var polygonsWithSources = List.of(withSources(SHAPE_X, upper), withSources(SHAPE_X, lower));

    when(grpcClientService.groupEqualPolygons(Map.of(id(upper), SHAPE_X, id(lower), SHAPE_X)))
        .thenReturn(List.of(List.of(id(upper))));

    assertThatThrownBy(() -> mergeService.mergeWithDepths(polygonsWithSources))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Polygon %s was not grouped".formatted(id(lower)));
  }

  @Test
  void mergeWithDepths_whenStackedPolygonMeetsPolygonCheckedBeforeStacking_thenStacksAllThree() {
    var above = polygon(0L, -100L);
    var below = polygon(-200L, -300L);
    var middle = polygon(-100L, -200L);

    when(lineService.getLines(List.of(above, below, middle))).thenReturn(edges(above, below, middle));
    when(grpcClientService.groupEqualPolygons(Map.of(id(above), SHAPE_X, id(below), SHAPE_X, id(middle), SHAPE_X)))
        .thenReturn(List.of(List.of(id(above), id(below), id(middle))));

    var result = mergeService.mergeWithDepths(List.of(
        withSources(SHAPE_X, above),
        withSources(SHAPE_X, below),
        withSources(SHAPE_X, middle)
    ));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, above, middle, below)));
  }

  @Test
  void mergeWithDepths_whenNoDepths_thenUnionsPolygonsWithoutGroupingEqualPolygons() {
    var first = polygon(null, null);
    var second = polygon(null, null);

    when(grpcClientService.unionPolygons(List.of(SHAPE_X, SHAPE_Y))).thenReturn("union");
    when(grpcClientService.multiPartToSinglePart("union")).thenReturn(List.of("union"));

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, first), withSources(SHAPE_Y, second)));

    assertThat(result).isEqualTo(List.of(withSources("union", first, second)));
  }

  @Test
  void mergeWithDepths_whenSameDepthPolygonsAreDisjoint_thenEachPartKeepsItsOwnSource() {
    var first = polygon(0L, -100L);
    var second = polygon(0L, -100L);

    when(grpcClientService.unionPolygons(List.of(SHAPE_X, SHAPE_Y))).thenReturn("multipart");
    when(grpcClientService.multiPartToSinglePart("multipart")).thenReturn(List.of("part 1", "part 2"));
    when(grpcClientService.polygonContains("part 1", SHAPE_X)).thenReturn(true);
    when(grpcClientService.polygonContains("part 1", SHAPE_Y)).thenReturn(false);
    when(grpcClientService.polygonContains("part 2", SHAPE_X)).thenReturn(false);
    when(grpcClientService.polygonContains("part 2", SHAPE_Y)).thenReturn(true);

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, first), withSources(SHAPE_Y, second)));

    assertThat(result).isEqualTo(List.of(withSources("part 1", first), withSources("part 2", second)));
  }

  @Test
  void mergeWithDepths_whenSameDepthUnionEqualsShapeBelow_thenStacksUnionWithShapeBelowInNextRound() {
    var left = polygon(0L, -100L);
    var right = polygon(0L, -100L);
    var below = polygon(-100L, -200L);

    when(lineService.getLines(List.of(left, right, below))).thenReturn(edges(left, right, below));
    when(grpcClientService.groupEqualPolygons(Map.of(id(left), SHAPE_X, id(right), SHAPE_Y, id(below), "shape s")))
        .thenReturn(List.of(List.of(id(left)), List.of(id(right)), List.of(id(below))));
    when(grpcClientService.unionPolygons(List.of(SHAPE_X, SHAPE_Y))).thenReturn("union");
    when(grpcClientService.multiPartToSinglePart("union")).thenReturn(List.of("union"));
    when(grpcClientService.groupEqualPolygons(Map.of(id(below), "shape s", id(left), "union")))
        .thenReturn(List.of(List.of(id(below), id(left))));

    var result = mergeService.mergeWithDepths(List.of(
        withSources(SHAPE_X, left),
        withSources(SHAPE_Y, right),
        withSources("shape s", below)
    ));

    assertThat(result).isEqualTo(List.of(withSources("shape s", below, left, right)));
  }

  @Test
  void mergeWithDepths_whenStackedPolygonHasSameDepthRangeAsAnother_thenUnionsThem() {
    var leftUpper = polygon(0L, -100L);
    var leftLower = polygon(-100L, -200L);
    var right = polygon(0L, -200L);

    when(lineService.getLines(List.of(leftUpper, leftLower, right))).thenReturn(edges(leftUpper, leftLower));
    when(grpcClientService.groupEqualPolygons(Map.of(id(leftUpper), SHAPE_X, id(leftLower), SHAPE_X, id(right), SHAPE_Y)))
        .thenReturn(List.of(List.of(id(leftUpper), id(leftLower)), List.of(id(right))));
    when(grpcClientService.unionPolygons(List.of(SHAPE_X, SHAPE_Y))).thenReturn("union");
    when(grpcClientService.multiPartToSinglePart("union")).thenReturn(List.of("union"));

    var result = mergeService.mergeWithDepths(List.of(
        withSources(SHAPE_X, leftUpper),
        withSources(SHAPE_X, leftLower),
        withSources(SHAPE_Y, right)
    ));

    assertThat(result).isEqualTo(List.of(withSources("union", leftUpper, leftLower, right)));
  }

  @Test
  void mergeWithDepths_whenStackedLinesMatchButAttributesDiffer_thenPolygonsUnchanged() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

    when(lineService.getLines(List.of(upper, lower)))
        .thenReturn(List.of(line(upper, EDGE, NORTH_EDGE), line(lower, EDGE, OTHER_EDGE)));
    when(grpcClientService.groupEqualPolygons(Map.of(id(upper), SHAPE_X, id(lower), SHAPE_X)))
        .thenReturn(List.of(List.of(id(upper), id(lower))));
    stubSegmentComparison(SHAPE_X, SHAPE_X);

    var result = mergeService.mergeWithDepths(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_X, lower)));

    assertThat(result).isEqualTo(List.of(withSources(SHAPE_X, upper), withSources(SHAPE_X, lower)));
  }

  @Test
  void mergeWithDepths_whenStackedLinesDifferOnlyOnEdgeRemovedByUnion_thenStacksIntoOnePolygon() {
    var upperLeft = polygon(0L, -100L);
    var upperRight = polygon(0L, -100L);
    var lowerLeft = polygon(-100L, -200L);
    var lowerRight = polygon(-100L, -200L);

    when(lineService.getLines(List.of(upperLeft, upperRight, lowerLeft, lowerRight))).thenReturn(List.of(
        line(upperLeft, EDGE, NORTH_EDGE),
        line(upperRight, INTERNAL_EDGE, NORTH_EDGE),
        line(lowerLeft, EDGE, NORTH_EDGE),
        line(lowerRight, INTERNAL_EDGE, OTHER_EDGE)
    ));
    when(grpcClientService.groupEqualPolygons(Map.of(id(upperLeft), "upper", id(lowerLeft), "lower")))
        .thenReturn(List.of(List.of(id(upperLeft), id(lowerLeft))));
    stubSegmentComparison("upper", "lower");

    var result = mergeService.mergeWithDepths(List.of(
        withSources("upper", upperLeft, upperRight),
        withSources("lower", lowerLeft, lowerRight)
    ));

    assertThat(result).isEqualTo(List.of(withSources("upper", upperLeft, upperRight, lowerLeft, lowerRight)));
  }

  @Test
  void mergeWithDepths_whenStackedLinesCutDifferentlyAndAttributesMatch_thenStacksIntoOnePolygon() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

    stubCutDifferently(upper, lower, NORTH_EDGE);

    var result = mergeService.mergeWithDepths(List.of(withSources("upper", upper), withSources("lower", lower)));

    assertThat(result).isEqualTo(List.of(withSources("upper", upper, lower)));
  }

  @Test
  void mergeWithDepths_whenStackedLinesCutDifferentlyAndAttributesDiffer_thenPolygonsUnchanged() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

    stubCutDifferently(upper, lower, OTHER_EDGE);

    var result = mergeService.mergeWithDepths(List.of(withSources("upper", upper), withSources("lower", lower)));

    assertThat(result).isEqualTo(List.of(withSources("upper", upper), withSources("lower", lower)));
  }

  @Test
  void mergeWithDepths_whenStackedLinesCutDifferentlyAndSegmentOrphanedOnBoth_thenPolygonsUnchanged() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

    when(lineService.getLines(List.of(upper, lower))).thenReturn(List.of(
        line(upper, EDGE, NORTH_EDGE),
        line(lower, LEFT_PIECE, NORTH_EDGE),
        line(lower, RIGHT_PIECE, NORTH_EDGE)
    ));
    when(grpcClientService.groupEqualPolygons(Map.of(id(upper), "upper", id(lower), "lower")))
        .thenReturn(List.of(List.of(id(upper), id(lower))));
    when(grpcClientService.unionPolygons(List.of("upper", "lower"))).thenReturn("union");
    when(grpcClientService.explodePolygon("union")).thenReturn(List.of(LEFT_SEGMENT, RIGHT_SEGMENT));
    when(grpcClientService.findParentLines(anyMap(), eq(List.of(LEFT_SEGMENT, RIGHT_SEGMENT))))
        .thenReturn(new FindParentLineResponse(Map.of(), List.of(LEFT_SEGMENT, RIGHT_SEGMENT)));

    var result = mergeService.mergeWithDepths(List.of(withSources("upper", upper), withSources("lower", lower)));

    assertThat(result).isEqualTo(List.of(withSources("upper", upper), withSources("lower", lower)));
  }

  @Test
  void mergeIn2d_whenUnionIsSinglePart_thenPartHasEverySourceWithoutCheckingContainment() {
    var first = polygon(null, null);
    var second = polygon(null, null);

    when(grpcClientService.unionPolygons(List.of(SHAPE_X, SHAPE_Y))).thenReturn("union");
    when(grpcClientService.multiPartToSinglePart("union")).thenReturn(List.of("union"));

    var result = mergeService.mergeIn2d(List.of(withSources(SHAPE_X, first), withSources(SHAPE_Y, second)));

    assertThat(result).isEqualTo(List.of(withSources("union", first, second)));
    verify(grpcClientService, never()).polygonContains(any(), any());
  }

  @Test
  void mergeIn2d_whenUnionIsMultiPart_thenEachPartHasSourcesItContains() {
    var first = polygon(null, null);
    var second = polygon(null, null);
    var third = polygon(null, null);

    when(grpcClientService.unionPolygons(List.of(SHAPE_X, SHAPE_Y, SHAPE_Z))).thenReturn("multipart");
    when(grpcClientService.multiPartToSinglePart("multipart")).thenReturn(List.of("part 1", "part 2"));
    when(grpcClientService.polygonContains("part 1", SHAPE_X)).thenReturn(true);
    when(grpcClientService.polygonContains("part 1", SHAPE_Y)).thenReturn(false);
    when(grpcClientService.polygonContains("part 1", SHAPE_Z)).thenReturn(true);
    when(grpcClientService.polygonContains("part 2", SHAPE_X)).thenReturn(false);
    when(grpcClientService.polygonContains("part 2", SHAPE_Y)).thenReturn(true);
    when(grpcClientService.polygonContains("part 2", SHAPE_Z)).thenReturn(false);

    var result = mergeService.mergeIn2d(List.of(
        withSources(SHAPE_X, first),
        withSources(SHAPE_Y, second),
        withSources(SHAPE_Z, third)
    ));

    assertThat(result).isEqualTo(List.of(withSources("part 1", first, third), withSources("part 2", second)));
  }

  // Upper keeps the whole edge while lower's edge is cut in two, with the right piece taking rightPieceAttributes
  private void stubCutDifferently(Polygon upper, Polygon lower, Map<String, Object> rightPieceAttributes) {
    when(lineService.getLines(List.of(upper, lower))).thenReturn(List.of(
        line(upper, EDGE, NORTH_EDGE),
        line(lower, LEFT_PIECE, NORTH_EDGE),
        line(lower, RIGHT_PIECE, rightPieceAttributes)
    ));
    when(grpcClientService.groupEqualPolygons(Map.of(id(upper), "upper", id(lower), "lower")))
        .thenReturn(List.of(List.of(id(upper), id(lower))));
    stubSegmentComparison("upper", "lower");
  }

  private void stubSegmentComparison(String firstEsriJson, String secondEsriJson) {
    when(grpcClientService.unionPolygons(List.of(firstEsriJson, secondEsriJson))).thenReturn("union");
    when(grpcClientService.explodePolygon("union")).thenReturn(List.of(LEFT_SEGMENT, RIGHT_SEGMENT));
    when(grpcClientService.findParentLines(anyMap(), eq(List.of(LEFT_SEGMENT, RIGHT_SEGMENT))))
        .thenAnswer(invocation -> groupSegmentsByParentId(invocation.getArgument(0)));
  }

  // Stands in for the node server: each segment goes under the id of a parent line it sits within
  private static FindParentLineResponse groupSegmentsByParentId(Map<String, UUID> parentEsriJsonToId) {
    var segmentToParentId = new LinkedHashMap<String, UUID>();
    parentEsriJsonToId.forEach((parentEsriJson, id) ->
        LINE_TO_SEGMENTS_WITHIN.get(parentEsriJson).forEach(segment -> segmentToParentId.putIfAbsent(segment, id)));
    return new FindParentLineResponse(segmentToParentId, List.of());
  }

  private static List<Line> edges(Polygon... polygons) {
    return Arrays.stream(polygons)
        .map(polygon -> line(polygon, EDGE, NORTH_EDGE))
        .toList();
  }

  private static Line line(Polygon polygon, String esriJson, Map<String, Object> attributes) {
    return LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withEsriJson(esriJson)
        .withAttributes(attributes)
        .build();
  }

  private static Polygon polygon(Long startDepth, Long endDepth) {
    return PolygonTestUtil.newBuilder()
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();
  }

  private static EsriJsonPolygonWithSources withSources(String esriJson, Polygon... sources) {
    return new EsriJsonPolygonWithSources(esriJson, List.of(sources));
  }

  private static String id(Polygon polygon) {
    return polygon.getId().toString();
  }
}
