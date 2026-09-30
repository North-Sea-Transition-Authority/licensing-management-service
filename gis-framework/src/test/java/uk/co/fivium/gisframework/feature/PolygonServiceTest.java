package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.CoordinateSystem;

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

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private PolygonService polygonService;

  @Test
  void savePolygon() {
    var polygon = PolygonTestUtil.newBuilder().build();

    polygonService.savePolygon(polygon);

    verify(polygonRepository).save(polygon);
  }


  @Test
  void savePolygons() {
    var polygon = PolygonTestUtil.newBuilder().build();

    polygonService.savePolygons(List.of(polygon));

    verify(polygonRepository).saveAll(List.of(polygon));
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
    var line1 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withDisplayOrder(1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withDisplayOrder(2).build();
    var line3 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withDisplayOrder(3).build();
    var entityBackedFeature = new EntityBackedFeature(
        feature,
        Map.of(polygon, List.of(line2, line3, line1))
    );
    var polygonEsriJson = "polygonEsriJson";

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(grpcClientService.buildPolygon(
        List.of(line1.getEsriJson(), line2.getEsriJson(), line3.getEsriJson()),
        feature.getCoordinateSystem(), false)
    ).thenReturn(polygonEsriJson);

    assertThat(polygonService.getPolygonsAsEsriJson(feature)).containsExactly(polygonEsriJson);
  }

  @ValueSource(booleans = {true, false})
  @ParameterizedTest
  void getPolygonsAsEsriJson_projectToWgs84Override_assertLinesSorted(boolean projectToWgs84) {
    var feature = FeatureTestUtil.newBuilder().build();
    var polygon = PolygonTestUtil.newBuilder().build();
    var line1 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withDisplayOrder(1).build();
    var line2 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withDisplayOrder(2).build();
    var line3 = LineTestUtil.newBuilder().withPolygon(polygon).withRingNumber(1).withDisplayOrder(3).build();
    var entityBackedFeature = new EntityBackedFeature(
        feature,
        Map.of(polygon, List.of(line2, line3, line1))
    );
    var polygonEsriJson = "polygonEsriJson";

    when(featureService.getEntityBackedFeature(feature)).thenReturn(entityBackedFeature);
    when(grpcClientService.buildPolygon(
        List.of(line1.getEsriJson(), line2.getEsriJson(), line3.getEsriJson()),
        feature.getCoordinateSystem(), projectToWgs84)
    ).thenReturn(polygonEsriJson);

    assertThat(polygonService.getPolygonsAsEsriJson(feature, projectToWgs84)).containsExactly(polygonEsriJson);
  }

  @Test
  void getPolygons() {
    when(polygonRepository.findAllByFeatureIn(FEATURES)).thenReturn(POLYGONS);
    assertThat(polygonService.getPolygons(FEATURES)).containsExactlyInAnyOrder(POLYGON_1, POLYGON_2);
  }

  @Test
  void getFeaturesAsWgs84EsriJson_assertEsriJsonFeaturesAndSpatialReference() throws Exception {
    var feature1 = FeatureTestUtil.newBuilder().withFeatureName("Feature 1").build();
    var feature2 = FeatureTestUtil.newBuilder().withFeatureName("Feature 2").build();
    var shallowPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature1)
        .withStartDepth(0L)
        .withEndDepth(-100L)
        .build();
    var deepPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature1)
        .withStartDepth(-100L)
        .withEndDepth(-200L)
        .build();
    var undefinedDepthPolygon = PolygonTestUtil.newBuilder()
        .withFeature(feature2)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();
    var shallowLine = LineTestUtil.newBuilder()
        .withPolygon(shallowPolygon)
        .withRingNumber(1)
        .withDisplayOrder(1)
        .withEsriJson("shallowLineEsriJson")
        .build();
    var deepLine = LineTestUtil.newBuilder()
        .withPolygon(deepPolygon)
        .withRingNumber(1)
        .withDisplayOrder(1)
        .withEsriJson("deepLineEsriJson")
        .build();
    var undefinedDepthLine = LineTestUtil.newBuilder()
        .withPolygon(undefinedDepthPolygon)
        .withRingNumber(1)
        .withDisplayOrder(1)
        .withEsriJson("undefinedDepthLineEsriJson")
        .build();
    var entityBackedFeature1 = new EntityBackedFeature(
        feature1,
        Map.of(shallowPolygon, List.of(shallowLine), deepPolygon, List.of(deepLine)));
    var entityBackedFeature2 = new EntityBackedFeature(
        feature2,
        Map.of(undefinedDepthPolygon, List.of(undefinedDepthLine)));
    var shallowGeometry = Map.<String, Object>of("rings", List.of("shallow"));
    var deepGeometry = Map.<String, Object>of("rings", List.of("deep"));
    var undefinedDepthGeometry = Map.<String, Object>of("rings", List.of("undefinedDepth"));

    when(featureService.getEntityBackedFeature(feature1)).thenReturn(entityBackedFeature1);
    when(featureService.getEntityBackedFeature(feature2)).thenReturn(entityBackedFeature2);
    when(grpcClientService.buildPolygon(List.of(shallowLine.getEsriJson()), feature1.getCoordinateSystem(), true))
        .thenReturn("shallowPolygonEsriJson");
    when(grpcClientService.buildPolygon(List.of(deepLine.getEsriJson()), feature1.getCoordinateSystem(), true))
        .thenReturn("deepPolygonEsriJson");
    when(grpcClientService.buildPolygon(List.of(undefinedDepthLine.getEsriJson()), feature2.getCoordinateSystem(), true))
        .thenReturn("undefinedDepthPolygonEsriJson");
    when(objectMapper.readValue(eq("shallowPolygonEsriJson"), any(TypeReference.class))).thenReturn(shallowGeometry);
    when(objectMapper.readValue(eq("deepPolygonEsriJson"), any(TypeReference.class))).thenReturn(deepGeometry);
    when(objectMapper.readValue(eq("undefinedDepthPolygonEsriJson"), any(TypeReference.class)))
        .thenReturn(undefinedDepthGeometry);

    var result = polygonService.getFeaturesAsWgs84EsriJson(List.of(feature1, feature2));

    assertThat(result.features()).containsExactlyInAnyOrder(
        new JsonFeature(shallowGeometry, new JsonFeature.Attributes(
            feature1.getId().toString(), "Feature 1", shallowPolygon.getId().toString(), 0L, -100L)),
        new JsonFeature(deepGeometry, new JsonFeature.Attributes(
            feature1.getId().toString(), "Feature 1", deepPolygon.getId().toString(), -100L, -200L)),
        new JsonFeature(undefinedDepthGeometry, new JsonFeature.Attributes(
            feature2.getId().toString(), "Feature 2", undefinedDepthPolygon.getId().toString(), null, null))
    );
    assertThat(result.spatialReference()).isEqualTo(JsonFeatures.SpatialReference.from(CoordinateSystem.WGS84));
  }

  @Test
  void deleteAll() {
    polygonService.deleteAll();
    verify(polygonRepository).deleteAll();
  }
}
