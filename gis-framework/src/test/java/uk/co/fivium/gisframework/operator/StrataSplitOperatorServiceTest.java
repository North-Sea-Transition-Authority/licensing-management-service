package uk.co.fivium.gisframework.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;

@ExtendWith(MockitoExtension.class)
class StrataSplitOperatorServiceTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @Captor
  private ArgumentCaptor<Map<Polygon, Polygon>> abovePolygonsCaptor;

  @Captor
  private ArgumentCaptor<Map<Polygon, Polygon>> belowPolygonsCaptor;

  @InjectMocks
  private StrataSplitOperatorService strataSplitOperatorService;

  @Test
  void strataSplitPolygon_whenOnePolygonSpansDepth_thenSplitsItAndCopiesOthersToTheirSide() {
    var feature = FeatureTestUtil.newBuilder().build();
    var abovePolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withAttributes(Map.of("polygon", "above"))
        .withStartDepth(0L)
        .withEndDepth(-1000L)
        .build();
    var spanningPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withAttributes(Map.of("polygon", "spanning"))
        .withStartDepth(-1000L)
        .withEndDepth(-3000L)
        .build();
    var belowPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withAttributes(Map.of("polygon", "below"))
        .withStartDepth(-3000L)
        .withEndDepth(null)
        .build();
    var entityBackedFeature = getEntityBackedFeature(feature, abovePolygon, spanningPolygon, belowPolygon);
    var aboveFeature = FeatureTestUtil.newBuilder().build();
    var belowFeature = FeatureTestUtil.newBuilder().build();

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(operatorResultProcessingService.processStrataSplitFeature(
        eq(entityBackedFeature), abovePolygonsCaptor.capture(), eq("above"))).thenReturn(aboveFeature);
    when(operatorResultProcessingService.processStrataSplitFeature(
        eq(entityBackedFeature), belowPolygonsCaptor.capture(), eq("below"))).thenReturn(belowFeature);

    var result = strataSplitOperatorService.strataSplitPolygon(feature, -2000L);

    assertThat(result).containsExactly(aboveFeature, belowFeature);

    var aboveSourceToNewPolygon = getSourceToNewPolygon(abovePolygonsCaptor.getValue());
    assertThat(aboveSourceToNewPolygon).containsOnlyKeys(abovePolygon, spanningPolygon);
    assertThat(aboveSourceToNewPolygon.get(abovePolygon)).usingRecursiveComparison()
        .isEqualTo(getExpectedPolygon(Map.of("polygon", "above"), 0L, -1000L));
    assertThat(aboveSourceToNewPolygon.get(spanningPolygon)).usingRecursiveComparison()
        .isEqualTo(getExpectedPolygon(Map.of("polygon", "spanning"), -1000L, -2000L));

    var belowSourceToNewPolygon = getSourceToNewPolygon(belowPolygonsCaptor.getValue());
    assertThat(belowSourceToNewPolygon).containsOnlyKeys(spanningPolygon, belowPolygon);
    assertThat(belowSourceToNewPolygon.get(spanningPolygon)).usingRecursiveComparison()
        .isEqualTo(getExpectedPolygon(Map.of("polygon", "spanning"), -2000L, -3000L));
    assertThat(belowSourceToNewPolygon.get(belowPolygon)).usingRecursiveComparison()
        .isEqualTo(getExpectedPolygon(Map.of("polygon", "below"), -3000L, null));
  }

  @Test
  void strataSplitPolygon_whenPolygonHasNoDepths_thenKeepsNullDepthsOnTheSideThatKeepsThem() {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();
    var entityBackedFeature = getEntityBackedFeature(feature, polygon);

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(operatorResultProcessingService.processStrataSplitFeature(
        eq(entityBackedFeature), abovePolygonsCaptor.capture(), eq("above")))
        .thenReturn(FeatureTestUtil.newBuilder().build());
    when(operatorResultProcessingService.processStrataSplitFeature(
        eq(entityBackedFeature), belowPolygonsCaptor.capture(), eq("below")))
        .thenReturn(FeatureTestUtil.newBuilder().build());

    strataSplitOperatorService.strataSplitPolygon(feature, -500L);

    assertThat(getSourceToNewPolygon(abovePolygonsCaptor.getValue()).get(polygon)).usingRecursiveComparison()
        .isEqualTo(getExpectedPolygon(Map.of(), null, -500L));
    assertThat(getSourceToNewPolygon(belowPolygonsCaptor.getValue()).get(polygon)).usingRecursiveComparison()
        .isEqualTo(getExpectedPolygon(Map.of(), -500L, null));
  }

  @Test
  void strataSplitPolygon_whenPolygonIsSplit_thenCopiesAttributesRatherThanSharingThem() {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withAttributes(Map.of("polygon", "spanning"))
        .withStartDepth(0L)
        .withEndDepth(-1000L)
        .build();
    var entityBackedFeature = getEntityBackedFeature(feature, polygon);

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(operatorResultProcessingService.processStrataSplitFeature(
        eq(entityBackedFeature), abovePolygonsCaptor.capture(), eq("above")))
        .thenReturn(FeatureTestUtil.newBuilder().build());
    when(operatorResultProcessingService.processStrataSplitFeature(
        eq(entityBackedFeature), belowPolygonsCaptor.capture(), eq("below")))
        .thenReturn(FeatureTestUtil.newBuilder().build());

    strataSplitOperatorService.strataSplitPolygon(feature, -500L);

    var abovePolygonAttributes = getSourceToNewPolygon(abovePolygonsCaptor.getValue()).get(polygon).getAttributes();
    var belowPolygonAttributes = getSourceToNewPolygon(belowPolygonsCaptor.getValue()).get(polygon).getAttributes();
    assertThat(abovePolygonAttributes)
        .isNotSameAs(polygon.getAttributes())
        .isNotSameAs(belowPolygonAttributes);
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -2500L, -2750L, -3000L, -3500L, -4000L})
  void strataSplitPolygon_whenNoPolygonCanBeSplitAtDepth_thenReturnsEmptyList(long depth) {
    var feature = FeatureTestUtil.newBuilder().build();
    var upperPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(0L)
        .withEndDepth(-2500L)
        .build();
    var lowerPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(-3000L)
        .withEndDepth(-3500L)
        .build();

    when(featureService.getEntityBackedFeature(feature))
        .thenReturn(getEntityBackedFeature(feature, upperPolygon, lowerPolygon));

    var result = strataSplitOperatorService.strataSplitPolygon(feature, depth);

    assertThat(result).isEmpty();
    verifyNoInteractions(operatorResultProcessingService);
  }

  private EntityBackedFeature getEntityBackedFeature(
      Feature feature,
      Polygon... polygons
  ) {
    Map<Polygon, List<Line>> polygonToLines = new LinkedHashMap<>();
    for (var polygon : polygons) {
      polygonToLines.put(polygon, List.of());
    }
    return new EntityBackedFeature(feature, polygonToLines);
  }

  private Map<Polygon, Polygon> getSourceToNewPolygon(Map<Polygon, Polygon> newPolygonToSource) {
    Map<Polygon, Polygon> sourceToNewPolygon = new HashMap<>();
    newPolygonToSource.forEach((newPolygon, source) -> sourceToNewPolygon.put(source, newPolygon));
    return sourceToNewPolygon;
  }

  private Polygon getExpectedPolygon(Map<String, Object> attributes, Long startDepth, Long endDepth) {
    var polygon = new Polygon();
    polygon.setAttributes(attributes);
    polygon.setStartDepth(startDepth);
    polygon.setEndDepth(endDepth);
    return polygon;
  }
}
