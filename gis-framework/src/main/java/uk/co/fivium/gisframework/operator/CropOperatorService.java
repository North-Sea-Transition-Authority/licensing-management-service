package uk.co.fivium.gisframework.operator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.EntityBackedFeature;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.grpc.FeatureCropResultDto;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.grpc.gis.IntersectionStatus;

@Service
public class CropOperatorService {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final GrpcClientService grpcClientService;
  private final OperatorResultProcessingService operatorResultProcessingService;

  public CropOperatorService(
      FeatureService featureService,
      PolygonService polygonService,
      GrpcClientService grpcClientService,
      OperatorResultProcessingService operatorResultProcessingService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.grpcClientService = grpcClientService;
    this.operatorResultProcessingService = operatorResultProcessingService;
  }

  /**
   * Crops each feature to the area of the boundary feature. A feature that lies across the boundary has a new feature
   * created from the part of it inside the boundary; the features passed in are never changed.
   *
   * @param boundaryFeature The feature that forms the boundary.
   * @param featuresToBeCropped The features to crop.
   * @return A crop result for each of the features.
   * @throws IllegalArgumentException if any feature is not in the boundary feature's coordinate system.
   */
  @Transactional
  public List<CropResultDto> cropFeaturesToBoundary(
      Feature boundaryFeature,
      Collection<Feature> featuresToBeCropped
  ) {
    if (featuresToBeCropped.isEmpty()) {
      return List.of();
    }

    var boundaryCoordinateSystem = boundaryFeature.getCoordinateSystem();
    if (featuresToBeCropped.stream().anyMatch(feature -> feature.getCoordinateSystem() != boundaryCoordinateSystem)) {
      throw new IllegalArgumentException(
          "All features to be cropped must use the boundary feature's coordinate system %s"
              .formatted(boundaryCoordinateSystem));
    }

    var esriJsonBoundaryPolygons = polygonService.getPolygonsAsEsriJson(boundaryFeature);

    var entityBackedFeatures = featureService.getEntityBackedFeatures(featuresToBeCropped);

    Map<UUID, Map<UUID, String>> featureIdToPolygonIdToEsriJson = entityBackedFeatures.stream()
        .collect(Collectors.toMap(
            entityBackedFeature -> entityBackedFeature.feature().getId(),
            polygonService::getPolygonIdToEsriJson
        ));

    var featureIdToCropResult =
        grpcClientService.cropToBoundary(esriJsonBoundaryPolygons, featureIdToPolygonIdToEsriJson);

    return entityBackedFeatures.stream()
        .map(entityBackedFeature -> toCropResultDto(
            entityBackedFeature,
            featureIdToCropResult.get(entityBackedFeature.feature().getId())))
        .toList();
  }

  private CropResultDto toCropResultDto(
      EntityBackedFeature entityBackedFeature,
      FeatureCropResultDto cropResult
  ) {
    if (cropResult.intersectionStatus() != IntersectionStatus.CROPPED) {
      return new CropResultDto(entityBackedFeature.feature().getId(), cropResult.intersectionStatus(), null);
    }

    var polygonIdToCroppedEsriJson = cropResult.polygonIdToCropResult()
        .entrySet()
        .stream()
        .filter(polygonIdToCropResult -> polygonIdToCropResult.getValue().status() == IntersectionStatus.CROPPED)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            polygonIdToCropResult -> polygonIdToCropResult.getValue().esriJsonCroppedPolygon()
        ));

    Set<UUID> removedPolygonIds = cropResult.polygonIdToCropResult()
        .entrySet()
        .stream()
        .filter(polygonIdToCropResult -> polygonIdToCropResult.getValue().status() == IntersectionStatus.FULLY_OUTSIDE)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());

    var croppedFeature = operatorResultProcessingService.processCroppedFeature(
        entityBackedFeature,
        polygonIdToCroppedEsriJson,
        removedPolygonIds
    );

    return new CropResultDto(
        entityBackedFeature.feature().getId(),
        IntersectionStatus.CROPPED,
        croppedFeature.getId()
    );
  }
}
