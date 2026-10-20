package uk.co.fivium.gisframework.relationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.feature.PolygonTestUtil;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.ValidationResponse;

@ExtendWith(MockitoExtension.class)
class FeatureRelationshipServiceTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private GrpcClientService grpcClientService;

  @InjectMocks
  private FeatureRelationshipService featureRelationshipService;

  @Test
  void get2dOverlappingFeatures_whenNoOtherFeatures_thenNoFeatures() {
    var feature = FeatureTestUtil.newBuilder().build();

    var result = featureRelationshipService.get2dOverlappingFeatures(feature, List.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenOtherFeatureCoordinateSystemDiffers_thenThrowsException() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var matchingFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var differingFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .build();
    var otherFeatures = List.of(matchingFeature, differingFeature);

    assertThatThrownBy(() -> featureRelationshipService.get2dOverlappingFeatures(feature, otherFeatures))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to check for overlap must use the feature's coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenFeatureHasNoPolygons_thenThrowsException() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeatures = List.of(FeatureTestUtil.newBuilder().build());

    when(polygonService.getPolygonsAsEsriJson(feature)).thenReturn(List.of());

    assertThatThrownBy(() -> featureRelationshipService.get2dOverlappingFeatures(feature, otherFeatures))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Feature %s has no polygons".formatted(feature.getId()));
    verifyNoInteractions(featureService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenSomeFeaturesOverlap_thenOverlappingFeaturesReturnedInGivenOrder() {
    var feature = FeatureTestUtil.newBuilder().build();
    var firstOverlappingFeature = FeatureTestUtil.newBuilder().build();
    var separateFeature = FeatureTestUtil.newBuilder().build();
    var secondOverlappingFeature = FeatureTestUtil.newBuilder().build();
    var otherFeatures = List.of(firstOverlappingFeature, separateFeature, secondOverlappingFeature);
    var entityBackedFeatures = List.of(
        new EntityBackedFeature(secondOverlappingFeature, Map.of()),
        new EntityBackedFeature(separateFeature, Map.of()),
        new EntityBackedFeature(firstOverlappingFeature, Map.of())
    );
    Map<UUID, Map<UUID, String>> featureIdToPolygonIdToEsriJson = Map.of(
        firstOverlappingFeature.getId(), Map.of(UUID.randomUUID(), "first overlapping polygon"),
        separateFeature.getId(), Map.of(UUID.randomUUID(), "separate polygon"),
        secondOverlappingFeature.getId(), Map.of(UUID.randomUUID(), "second overlapping polygon")
    );

    when(polygonService.getPolygonsAsEsriJson(feature)).thenReturn(List.of("first polygon", "second polygon"));
    when(featureService.getEntityBackedFeatures(otherFeatures)).thenReturn(entityBackedFeatures);
    when(polygonService.getFeatureIdToPolygonIdToEsriJson(entityBackedFeatures))
        .thenReturn(featureIdToPolygonIdToEsriJson);
    when(grpcClientService.find2dOverlappingFeatures(
        List.of("first polygon", "second polygon"),
        featureIdToPolygonIdToEsriJson
    ))
        .thenReturn(Set.of(secondOverlappingFeature.getId(), firstOverlappingFeature.getId()));

    var result = featureRelationshipService.get2dOverlappingFeatures(feature, otherFeatures);

    assertThat(result).containsExactly(firstOverlappingFeature, secondOverlappingFeature);
  }

  @Test
  void is2dOverlapping_whenFeaturesOverlap_thenTrue() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var entityBackedFeatures = List.of(new EntityBackedFeature(otherFeature, Map.of()));
    Map<UUID, Map<UUID, String>> featureIdToPolygonIdToEsriJson =
        Map.of(otherFeature.getId(), Map.of(UUID.randomUUID(), "other polygon"));

    when(polygonService.getPolygonsAsEsriJson(feature)).thenReturn(List.of("polygon"));
    when(featureService.getEntityBackedFeatures(List.of(otherFeature))).thenReturn(entityBackedFeatures);
    when(polygonService.getFeatureIdToPolygonIdToEsriJson(entityBackedFeatures))
        .thenReturn(featureIdToPolygonIdToEsriJson);
    when(grpcClientService.find2dOverlappingFeatures(List.of("polygon"), featureIdToPolygonIdToEsriJson))
        .thenReturn(Set.of(otherFeature.getId()));

    var result = featureRelationshipService.is2dOverlapping(feature, otherFeature);

    assertThat(result).isTrue();
  }

  @Test
  void is2dOverlapping_whenFeaturesDoNotOverlap_thenFalse() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var entityBackedFeatures = List.of(new EntityBackedFeature(otherFeature, Map.of()));
    Map<UUID, Map<UUID, String>> featureIdToPolygonIdToEsriJson =
        Map.of(otherFeature.getId(), Map.of(UUID.randomUUID(), "other polygon"));

    when(polygonService.getPolygonsAsEsriJson(feature)).thenReturn(List.of("polygon"));
    when(featureService.getEntityBackedFeatures(List.of(otherFeature))).thenReturn(entityBackedFeatures);
    when(polygonService.getFeatureIdToPolygonIdToEsriJson(entityBackedFeatures))
        .thenReturn(featureIdToPolygonIdToEsriJson);
    when(grpcClientService.find2dOverlappingFeatures(List.of("polygon"), featureIdToPolygonIdToEsriJson))
        .thenReturn(Set.of());

    var result = featureRelationshipService.is2dOverlapping(feature, otherFeature);

    assertThat(result).isFalse();
  }

  @Test
  void isTopologicallyEqual_whenNoFeatures_thenThrowsException() {
    var otherFeature = FeatureTestUtil.newBuilder().build();

    assertThatThrownBy(() -> featureRelationshipService.isTopologicallyEqual(List.of(), otherFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Must provide at least one feature to compare");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void isTopologicallyEqual_whenFeatureCoordinateSystemDiffers_thenThrowsException() {
    var otherFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var matchingFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var differingFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .build();
    var features = List.of(matchingFeature, differingFeature);

    assertThatThrownBy(() -> featureRelationshipService.isTopologicallyEqual(features, otherFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to compare must use the other feature's coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isTopologicallyEqual_whenFeaturesShareCoordinateSystem_thenValidationResultReturned(boolean isValid) {
    var firstFeature = FeatureTestUtil.newBuilder().build();
    var secondFeature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var firstPolygon = PolygonTestUtil.newBuilder().withFeature(firstFeature).build();
    var secondPolygon = PolygonTestUtil.newBuilder().withFeature(secondFeature).build();
    var features = List.of(firstFeature, secondFeature);
    var otherEntityBackedFeature = new EntityBackedFeature(otherFeature, Map.of());
    when(featureService.getEntityBackedFeatures(features)).thenReturn(List.of(
        new EntityBackedFeature(firstFeature, Map.of(firstPolygon, List.of(
            LineTestUtil.newBuilder().withPolygon(firstPolygon).withEsriJson("first line").build()))),
        new EntityBackedFeature(secondFeature, Map.of(secondPolygon, List.of(
            LineTestUtil.newBuilder().withPolygon(secondPolygon).withEsriJson("second line").build())))
    ));
    when(featureService.getEntityBackedFeature(otherFeature)).thenReturn(otherEntityBackedFeature);
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("first line"), List.of("second line")),
        otherEntityBackedFeature
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(isValid).build());

    var result = featureRelationshipService.isTopologicallyEqual(features, otherFeature);

    assertThat(result).isEqualTo(isValid);
  }
}
