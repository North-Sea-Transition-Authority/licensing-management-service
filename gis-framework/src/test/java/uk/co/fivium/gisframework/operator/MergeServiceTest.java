package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

  private static final String SHAPE_X = "shape x";
  private static final String SHAPE_Y = "shape y";
  private static final String SHAPE_Z = "shape z";

  @Mock
  private GrpcClientService grpcClientService;

  @InjectMocks
  private MergeService mergeService;

  @Test
  void mergeWithDepths_whenSameShapeAndDepthsMeet_thenStacksIntoOnePolygon() {
    var upper = polygon(0L, -100L);
    var lower = polygon(-100L, -200L);

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
