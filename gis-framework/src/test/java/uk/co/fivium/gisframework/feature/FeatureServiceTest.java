package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

  private static final Feature FEATURE = FeatureTestUtil.newBuilder().build();

  @Mock
  private FeatureRepository featureRepository;

  @Mock
  private PolygonRepository polygonRepository;

  @Mock
  private LineRepository lineRepository;

  @InjectMocks
  private FeatureService featureService;

  @Test
  void saveFeature() {
    featureService.saveFeature(FEATURE);

    verify(featureRepository).save(FEATURE);
  }

  @Test
  void findAllByParentFeature() {
    var childFeature1 = FeatureTestUtil.newBuilder().build();
    var childFeature2 = FeatureTestUtil.newBuilder().build();

    when(featureRepository.findAllByParentFeatureId(FEATURE.getId()))
        .thenReturn(List.of(childFeature1, childFeature2));

    var result = featureService.findAllByParentFeature(FEATURE);

    var expected = List.of(childFeature1, childFeature2);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void findAllByParentFeature_whenNoResult_thenReturnsEmptyList() {
    when(featureRepository.findAllByParentFeatureId(FEATURE.getId()))
        .thenReturn(List.of());

    var result = featureService.findAllByParentFeature(FEATURE);

    assertThat(result).isEmpty();
  }

  @Test
  void getEntityBackedFeature() {
    var polygon1 = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();
    var polygon2 = PolygonTestUtil.newBuilder().withFeature(FEATURE).build();

    var line1 = LineTestUtil.newBuilder().withPolygon(polygon1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon2).build();

    when(polygonRepository.findAllByFeature(FEATURE)).thenReturn(List.of(polygon1, polygon2));
    when(lineRepository.findAllByPolygon(polygon1)).thenReturn(List.of(line1));
    when(lineRepository.findAllByPolygon(polygon2)).thenReturn(List.of(line2));

    var result = featureService.getEntityBackedFeature(FEATURE);

    var expected = new EntityBackedFeature(FEATURE, Map.of(polygon1, List.of(line1), polygon2, List.of(line2)));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
