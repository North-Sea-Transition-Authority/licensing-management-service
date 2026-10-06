package uk.co.fivium.gisframework.operator;

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
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.FeatureTestUtil;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.FeatureCropResultDto;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.grpc.PolygonCropResultDto;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.IntersectionStatus;

@ExtendWith(MockitoExtension.class)
class CropOperatorServiceTest {

  @Mock
  private FeatureService featureService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private GrpcClientService grpcClientService;

  @Mock
  private OperatorResultProcessingService operatorResultProcessingService;

  @InjectMocks
  private CropOperatorService cropOperatorService;

  @Test
  void cropFeaturesToBoundary_whenNoFeatures_thenNoResults() {
    var boundaryFeature = FeatureTestUtil.newBuilder().build();

    var result = cropOperatorService.cropFeaturesToBoundary(boundaryFeature, List.of());

    assertThat(result).isEmpty();
    verifyNoInteractions(featureService, polygonService, grpcClientService, operatorResultProcessingService);
  }

  @Test
  void cropFeaturesToBoundary_whenFeatureCoordinateSystemDiffersFromBoundary_thenThrowsException() {
    var boundaryFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var matchingFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .build();
    var differingFeature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .build();
    var featuresToBeCropped = List.of(matchingFeature, differingFeature);

    assertThatThrownBy(() -> cropOperatorService.cropFeaturesToBoundary(boundaryFeature, featuresToBeCropped))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("All features to be cropped must use the boundary feature's coordinate system ED50");
    verifyNoInteractions(featureService, polygonService, grpcClientService, operatorResultProcessingService);
  }

  @ParameterizedTest
  @EnumSource(value = IntersectionStatus.class, names = {"FULLY_INSIDE", "FULLY_OUTSIDE"})
  void cropFeaturesToBoundary_whenFeatureNotCropped_thenNoCroppedFeatureCreated(IntersectionStatus status) {
    var boundaryFeature = FeatureTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var entityBackedFeature = new EntityBackedFeature(feature, Map.of());
    var polygonId = UUID.randomUUID();

    when(polygonService.getPolygonsAsEsriJson(boundaryFeature)).thenReturn(List.of("boundary polygon"));
    when(featureService.getEntityBackedFeatures(List.of(feature))).thenReturn(List.of(entityBackedFeature));
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(Map.of(polygonId, "polygon"));
    when(grpcClientService.cropToBoundary(List.of("boundary polygon"), Map.of(feature.getId(), Map.of(polygonId, "polygon"))))
        .thenReturn(Map.of(feature.getId(), new FeatureCropResultDto(status, Map.of())));

    var result = cropOperatorService.cropFeaturesToBoundary(boundaryFeature, List.of(feature));

    assertThat(result).containsExactly(new CropResultDto(feature.getId(), status, null));
    verifyNoInteractions(operatorResultProcessingService);
  }

  @Test
  void cropFeaturesToBoundary_whenFeatureCropped_thenCroppedFeatureCreatedWithoutPolygonsOutsideBoundary() {
    var boundaryFeature = FeatureTestUtil.newBuilder().build();
    var feature = FeatureTestUtil.newBuilder().build();
    var entityBackedFeature = new EntityBackedFeature(feature, Map.of());
    var croppedFeature = FeatureTestUtil.newBuilder().build();
    var croppedPolygonId = UUID.randomUUID();
    var outsidePolygonId = UUID.randomUUID();
    var polygonIdToEsriJson = Map.of(croppedPolygonId, "cropped polygon", outsidePolygonId, "outside polygon");

    when(polygonService.getPolygonsAsEsriJson(boundaryFeature))
        .thenReturn(List.of("first boundary polygon", "second boundary polygon"));
    when(featureService.getEntityBackedFeatures(List.of(feature))).thenReturn(List.of(entityBackedFeature));
    when(polygonService.getPolygonIdToEsriJson(entityBackedFeature)).thenReturn(polygonIdToEsriJson);
    when(grpcClientService.cropToBoundary(
        List.of("first boundary polygon", "second boundary polygon"),
        Map.of(feature.getId(), polygonIdToEsriJson)
    ))
        .thenReturn(Map.of(feature.getId(), new FeatureCropResultDto(IntersectionStatus.CROPPED, Map.of(
            croppedPolygonId, new PolygonCropResultDto(IntersectionStatus.CROPPED, "cropped result"),
            outsidePolygonId, new PolygonCropResultDto(IntersectionStatus.FULLY_OUTSIDE, "")
        ))));
    when(operatorResultProcessingService.processCroppedFeature(
        entityBackedFeature,
        Map.of(croppedPolygonId, "cropped result"),
        Set.of(outsidePolygonId)
    ))
        .thenReturn(croppedFeature);

    var result = cropOperatorService.cropFeaturesToBoundary(boundaryFeature, List.of(feature));

    assertThat(result).containsExactly(
        new CropResultDto(feature.getId(), IntersectionStatus.CROPPED, croppedFeature.getId())
    );
  }
}
