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
import uk.co.fivium.gisframework.grpc.GrpcClientService;

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

  @Mock
  private FeatureService featureService;

  @Mock
  private GrpcClientService grpcClientService;

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

  @Test
  void findAllByFeature() {
    when(polygonRepository.findAllByFeature(FEATURE_1)).thenReturn(List.of(POLYGON_1, POLYGON_2));

    var result = polygonService.findAllByFeature(FEATURE_1);

    var expected = List.of(POLYGON_1, POLYGON_2);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getPolygonsAsEsriJson_assertLinesSorted() {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().build();
    var line1 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withRingConnectionOrder(1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withRingConnectionOrder(2).build();
    var line3 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withRingConnectionOrder(3).build();
    var entityBackedFeature = new EntityBackedFeature(
        feature,
        Map.of(polygon, List.of(line2, line3, line1))
    );
    var polygonEsriJson = "polygonEsriJson";

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(grpcClientService.buildPolygon(
        List.of(line1.getEsriJson(), line2.getEsriJson(), line3.getEsriJson()),
        feature.getCoordinateSystem())
    ).thenReturn(polygonEsriJson);

    assertThat(polygonService.getPolygonsAsEsriJson(feature)).containsExactly(polygonEsriJson);
  }

  @Test
  void getPolygons() {
    when(polygonRepository.findAllByFeatureIn(FEATURES)).thenReturn(POLYGONS);
    assertThat(polygonService.getPolygons(FEATURES)).containsExactlyInAnyOrder(POLYGON_1, POLYGON_2);
  }

  @Test
  void deleteAll() {
    polygonService.deleteAll();
    verify(polygonRepository).deleteAll();
  }
}
