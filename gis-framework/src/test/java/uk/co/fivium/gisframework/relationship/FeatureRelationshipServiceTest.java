package uk.co.fivium.gisframework.relationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
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
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineTestUtil;
import uk.co.fivium.gisframework.feature.Polygon;
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
  void build2dPolygons_whenNoFeatures_thenNoPolygons() {
    var result = featureRelationshipService.build2dPolygons(List.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void build2dPolygons_whenFeatures_thenPolygonsKeyedByFeatureInGivenOrder() {
    var firstFeature = FeatureTestUtil.newBuilder().build();
    var featureWithoutPolygons = FeatureTestUtil.newBuilder().build();
    var secondFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(firstFeature, featureWithoutPolygons, secondFeature);
    var entityBackedFeatures = List.of(
        new EntityBackedFeature(firstFeature, Map.of()),
        new EntityBackedFeature(featureWithoutPolygons, Map.of()),
        new EntityBackedFeature(secondFeature, Map.of())
    );
    var firstPolygons = Map.of(UUID.randomUUID(), "first polygon");
    var secondPolygons = Map.of(UUID.randomUUID(), "second polygon");

    when(featureService.getEntityBackedFeatures(features)).thenReturn(entityBackedFeatures);
    when(polygonService.getFeatureIdToPolygonIdToEsriJson(entityBackedFeatures))
        .thenReturn(Map.of(secondFeature.getId(), secondPolygons, firstFeature.getId(), firstPolygons));

    var result = featureRelationshipService.build2dPolygons(features);

    assertThat(result).containsExactly(
        Map.entry(firstFeature, firstPolygons),
        Map.entry(secondFeature, secondPolygons)
    );
  }

  @Test
  void get2dOverlappingFeatures_whenNoFeaturesToUnion_thenThrowsException() {
    List<Feature> features = List.of();
    var otherFeaturePolygons = Map.of(
        FeatureTestUtil.newBuilder().build(),
        Map.of(UUID.randomUUID(), "other polygon")
    );

    assertThatThrownBy(() -> featureRelationshipService.get2dOverlappingFeatures(features, otherFeaturePolygons))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Must provide at least one feature to check for overlap");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenFeaturesToUnionInDifferentCoordinateSystems_thenThrowsException() {
    var features = List.of(
        FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build(),
        FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID).build()
    );
    Map<Feature, Map<UUID, String>> otherFeaturePolygons = Map.of();

    assertThatThrownBy(() -> featureRelationshipService.get2dOverlappingFeatures(features, otherFeaturePolygons))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to check for overlap must use the coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenOtherFeatureNotInCoordinateSystemOfFeaturesToUnion_thenThrowsException() {
    var features = List.of(FeatureTestUtil.newBuilder().withCoordinateSystem(CoordinateSystem.ED50).build());
    var otherFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .build();
    var otherFeaturePolygons = Map.of(otherFeature, Map.of(UUID.randomUUID(), "other polygon"));

    assertThatThrownBy(() -> featureRelationshipService.get2dOverlappingFeatures(features, otherFeaturePolygons))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to check for overlap must use the coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenNoOtherFeaturesToCheck_thenNoFeatures() {
    var features = List.of(FeatureTestUtil.newBuilder().build());

    var result = featureRelationshipService.get2dOverlappingFeatures(features, Map.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenFeaturesToUnionHaveNoPolygons_thenThrowsException() {
    var feature = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature);
    var otherFeaturePolygons = Map.of(
        FeatureTestUtil.newBuilder().build(),
        Map.of(UUID.randomUUID(), "other polygon")
    );
    var entityBackedFeature = new EntityBackedFeature(feature, Map.of());

    when(featureService.getEntityBackedFeatures(features)).thenReturn(List.of(entityBackedFeature));
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(Map.of());

    assertThatThrownBy(() -> featureRelationshipService.get2dOverlappingFeatures(features, otherFeaturePolygons))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Features to check for overlap have no polygons");
    verifyNoInteractions(grpcClientService);
  }

  @Test
  void get2dOverlappingFeatures_whenSeveralFeaturesToUnion_thenCheckedTogetherAndOverlappingFeaturesReturnedInGivenOrder() {
    var firstFeature = FeatureTestUtil.newBuilder().build();
    var secondFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(firstFeature, secondFeature);
    var firstEntityBackedFeature = new EntityBackedFeature(firstFeature, Map.of());
    var secondEntityBackedFeature = new EntityBackedFeature(secondFeature, Map.of());
    var firstOverlappingFeature = FeatureTestUtil.newBuilder().build();
    var separateFeature = FeatureTestUtil.newBuilder().build();
    var secondOverlappingFeature = FeatureTestUtil.newBuilder().build();
    var firstOverlappingPolygons = Map.of(UUID.randomUUID(), "first overlapping polygon");
    var separatePolygons = Map.of(UUID.randomUUID(), "separate polygon");
    var secondOverlappingPolygons = Map.of(UUID.randomUUID(), "second overlapping polygon");
    Map<Feature, Map<UUID, String>> otherFeaturePolygons = new LinkedHashMap<>();
    otherFeaturePolygons.put(firstOverlappingFeature, firstOverlappingPolygons);
    otherFeaturePolygons.put(separateFeature, separatePolygons);
    otherFeaturePolygons.put(secondOverlappingFeature, secondOverlappingPolygons);

    when(featureService.getEntityBackedFeatures(features))
        .thenReturn(List.of(firstEntityBackedFeature, secondEntityBackedFeature));
    when(polygonService.getPolygonIdToEsriJson(firstEntityBackedFeature))
        .thenReturn(Map.of(UUID.randomUUID(), "first polygon"));
    when(polygonService.getPolygonIdToEsriJson(secondEntityBackedFeature))
        .thenReturn(Map.of(UUID.randomUUID(), "second polygon"));
    when(grpcClientService.find2dOverlappingFeatures(
        List.of("first polygon", "second polygon"),
        Map.of(
            firstOverlappingFeature.getId(), firstOverlappingPolygons,
            separateFeature.getId(), separatePolygons,
            secondOverlappingFeature.getId(), secondOverlappingPolygons
        )
    ))
        .thenReturn(Set.of(secondOverlappingFeature.getId(), firstOverlappingFeature.getId()));

    var result = featureRelationshipService.get2dOverlappingFeatures(features, otherFeaturePolygons);

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
  void is2dTopologicallyEqual_whenNoFeatures_thenThrowsException() {
    var otherFeature = FeatureTestUtil.newBuilder().build();

    assertThatThrownBy(() -> featureRelationshipService.is2dTopologicallyEqual(List.of(), otherFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Must provide at least one feature to compare");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void is2dTopologicallyEqual_whenFeatureCoordinateSystemDiffers_thenThrowsException() {
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

    assertThatThrownBy(() -> featureRelationshipService.is2dTopologicallyEqual(features, otherFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to compare must use the other feature's coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void is2dTopologicallyEqual_whenFeaturesShareCoordinateSystem_thenValidationResultReturned(boolean isValid) {
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

    var result = featureRelationshipService.is2dTopologicallyEqual(features, otherFeature);

    assertThat(result).isEqualTo(isValid);
  }

  @Test
  void is3dTopologicallyEqual_whenNoFeatures_thenThrowsException() {
    var otherFeature = FeatureTestUtil.newBuilder().build();

    assertThatThrownBy(() -> featureRelationshipService.is3dTopologicallyEqual(List.of(), otherFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Must provide at least one feature to compare");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void is3dTopologicallyEqual_whenFeatureCoordinateSystemDiffers_thenThrowsException() {
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

    assertThatThrownBy(() -> featureRelationshipService.is3dTopologicallyEqual(features, otherFeature))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to compare must use the other feature's coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService);
  }

  @Test
  void is3dTopologicallyEqual_whenAllDepthsNull_thenComparedOnce() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature);
    var otherPolygonToLines = polygonToLines(polygon(otherFeature, null, null), "other line");

    when(featureService.getEntityBackedFeatures(features))
        .thenReturn(List.of(new EntityBackedFeature(feature, polygonToLines(polygon(feature, null, null), "line"))));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isTrue();
    verify(grpcClientService).validateTopologicallyEqual(
        List.of(List.of("line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    );
    verifyNoMoreInteractions(grpcClientService);
  }

  @Test
  void is3dTopologicallyEqual_whenLayeredFeaturesMatchSinglePolygon_thenTrue() {
    var upperFeature = FeatureTestUtil.newBuilder().build();
    var lowerFeature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(upperFeature, lowerFeature);
    var otherPolygonToLines = polygonToLines(polygon(otherFeature, 0L, -200L), "other line");

    when(featureService.getEntityBackedFeatures(features)).thenReturn(List.of(
        new EntityBackedFeature(upperFeature, polygonToLines(polygon(upperFeature, 0L, -100L), "upper line")),
        new EntityBackedFeature(lowerFeature, polygonToLines(polygon(lowerFeature, -100L, -200L), "lower line"))
    ));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("upper line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("lower line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isTrue();
  }

  @Test
  void is3dTopologicallyEqual_whenPolygonsAtSameDepthInDifferentFeatures_thenComparedTogether() {
    var westFeature = FeatureTestUtil.newBuilder().build();
    var eastFeature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(westFeature, eastFeature);
    var otherPolygonToLines = polygonToLines(polygon(otherFeature, 0L, -100L), "other line");

    when(featureService.getEntityBackedFeatures(features)).thenReturn(List.of(
        new EntityBackedFeature(westFeature, polygonToLines(polygon(westFeature, 0L, -100L), "west line")),
        new EntityBackedFeature(eastFeature, polygonToLines(polygon(eastFeature, 0L, -100L), "east line"))
    ));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("west line"), List.of("east line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isTrue();
  }

  @Test
  void is3dTopologicallyEqual_whenPolygonSpansSeveralLayers_thenUnionedIntoEachLayer() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature);
    Map<Polygon, List<Line>> featurePolygonToLines = new LinkedHashMap<>();
    featurePolygonToLines.putAll(polygonToLines(polygon(feature, 0L, -100L), "upper west line"));
    featurePolygonToLines.putAll(polygonToLines(polygon(feature, -100L, -200L), "lower west line"));
    featurePolygonToLines.putAll(polygonToLines(polygon(feature, 0L, -200L), "east line"));
    var otherPolygonToLines = polygonToLines(polygon(otherFeature, 0L, -200L), "other line");

    when(featureService.getEntityBackedFeatures(features))
        .thenReturn(List.of(new EntityBackedFeature(feature, featurePolygonToLines)));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("upper west line"), List.of("east line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("lower west line"), List.of("east line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isTrue();
  }

  @Test
  void is3dTopologicallyEqual_whenFirstDepthNotEqual_thenFalseWithoutCheckingDeeper() {
    var upperFeature = FeatureTestUtil.newBuilder().build();
    var lowerFeature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(upperFeature, lowerFeature);
    var otherPolygonToLines = polygonToLines(polygon(otherFeature, 0L, -200L), "other line");

    when(featureService.getEntityBackedFeatures(features)).thenReturn(List.of(
        new EntityBackedFeature(upperFeature, polygonToLines(polygon(upperFeature, 0L, -100L), "upper line")),
        new EntityBackedFeature(lowerFeature, polygonToLines(polygon(lowerFeature, -100L, -200L), "lower line"))
    ));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("upper line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(false).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isFalse();
    verify(grpcClientService).validateTopologicallyEqual(
        List.of(List.of("upper line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    );
    verifyNoMoreInteractions(grpcClientService);
  }

  @Test
  void is3dTopologicallyEqual_whenOtherFeatureHasDepthGap_thenFalse() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature);
    var otherUpperPolygonToLines = polygonToLines(polygon(otherFeature, 0L, -50L), "other upper line");
    Map<Polygon, List<Line>> otherPolygonToLines = new LinkedHashMap<>(otherUpperPolygonToLines);
    otherPolygonToLines.putAll(polygonToLines(polygon(otherFeature, -100L, -200L), "other lower line"));

    when(featureService.getEntityBackedFeatures(features))
        .thenReturn(List.of(new EntityBackedFeature(feature, polygonToLines(polygon(feature, 0L, -200L), "line"))));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("line")),
        new EntityBackedFeature(otherFeature, otherUpperPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isFalse();
    verify(grpcClientService).validateTopologicallyEqual(
        List.of(List.of("line")),
        new EntityBackedFeature(otherFeature, otherUpperPolygonToLines)
    );
    verifyNoMoreInteractions(grpcClientService);
  }

  @Test
  void is3dTopologicallyEqual_whenBothHaveSameDepthGap_thenTrue() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature);
    Map<Polygon, List<Line>> featurePolygonToLines = new LinkedHashMap<>();
    featurePolygonToLines.putAll(polygonToLines(polygon(feature, 0L, -50L), "upper line"));
    featurePolygonToLines.putAll(polygonToLines(polygon(feature, -100L, -200L), "lower line"));
    var otherUpperPolygonToLines = polygonToLines(polygon(otherFeature, 0L, -50L), "other upper line");
    var otherLowerPolygonToLines = polygonToLines(polygon(otherFeature, -100L, -200L), "other lower line");
    Map<Polygon, List<Line>> otherPolygonToLines = new LinkedHashMap<>(otherUpperPolygonToLines);
    otherPolygonToLines.putAll(otherLowerPolygonToLines);

    when(featureService.getEntityBackedFeatures(features))
        .thenReturn(List.of(new EntityBackedFeature(feature, featurePolygonToLines)));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("upper line")),
        new EntityBackedFeature(otherFeature, otherUpperPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("lower line")),
        new EntityBackedFeature(otherFeature, otherLowerPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isTrue();
  }

  @Test
  void is3dTopologicallyEqual_whenFeatureBoundedAndOtherFeatureInfinite_thenFalse() {
    var feature = FeatureTestUtil.newBuilder().build();
    var otherFeature = FeatureTestUtil.newBuilder().build();
    var features = List.of(feature);
    var otherPolygonToLines = polygonToLines(polygon(otherFeature, 0L, null), "other line");

    when(featureService.getEntityBackedFeatures(features))
        .thenReturn(List.of(new EntityBackedFeature(feature, polygonToLines(polygon(feature, 0L, -100L), "line"))));
    when(featureService.getEntityBackedFeature(otherFeature))
        .thenReturn(new EntityBackedFeature(otherFeature, otherPolygonToLines));
    when(grpcClientService.validateTopologicallyEqual(
        List.of(List.of("line")),
        new EntityBackedFeature(otherFeature, otherPolygonToLines)
    )).thenReturn(ValidationResponse.newBuilder().setIsValid(true).build());

    var result = featureRelationshipService.is3dTopologicallyEqual(features, otherFeature);

    assertThat(result).isFalse();
  }

  private Polygon polygon(Feature feature, Long startDepth, Long endDepth) {
    return PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();
  }

  private Map<Polygon, List<Line>> polygonToLines(Polygon polygon, String lineEsriJson) {
    return Map.of(polygon, List.of(LineTestUtil.newBuilder().withPolygon(polygon).withEsriJson(lineEsriJson).build()));
  }
}
