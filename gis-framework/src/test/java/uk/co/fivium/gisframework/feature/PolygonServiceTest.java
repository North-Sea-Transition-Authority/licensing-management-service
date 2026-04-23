package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolygonServiceTest {

  private static final Feature FEATURE_1 = FeatureTestUtil.newBuilder().build();
  private static final Feature FEATURE_2 = FeatureTestUtil.newBuilder().build();
  private static final List<Feature> FEATURES = List.of(FEATURE_1, FEATURE_2);

  private static final Polygon POLYGON_1 = PolygonTestUtil.newBuilder().build();
  private static final Polygon POLYGON_2 = PolygonTestUtil.newBuilder().build();
  private static final List<Polygon> POLYGONS = List.of(POLYGON_1, POLYGON_2);

  @Mock
  private PolygonRepository polygonRepository;

  @InjectMocks
  private PolygonService polygonService;

  @Test
  void savePolygon() {
    var polygon = PolygonTestUtil.newBuilder().build();

    polygonService.savePolygon(polygon);

    verify(polygonRepository).save(polygon);
  }

  @Test
  void findAllByFeatureIn() {
    when(polygonRepository.findAllByFeatureIn(FEATURES)).thenReturn(POLYGONS);

    var result = polygonService.findAllByFeatureIn(FEATURES);

    assertThat(result).usingRecursiveComparison().isEqualTo(POLYGONS);
  }

  @Test
  void findAllByFeatureIn_whenNoResult_thenReturnsEmptyList() {
    when(polygonRepository.findAllByFeatureIn(FEATURES)).thenReturn(List.of());

    var result = polygonService.findAllByFeatureIn(FEATURES);

    assertThat(result).isEmpty();
  }
}
