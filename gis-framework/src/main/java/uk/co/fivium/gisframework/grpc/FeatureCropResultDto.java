package uk.co.fivium.gisframework.grpc;

import java.util.Map;
import java.util.UUID;
import uk.co.fivium.grpc.gis.IntersectionStatus;

public record FeatureCropResultDto(
    IntersectionStatus intersectionStatus,
    Map<UUID, PolygonCropResultDto> polygonIdToCropResult
) {
}
