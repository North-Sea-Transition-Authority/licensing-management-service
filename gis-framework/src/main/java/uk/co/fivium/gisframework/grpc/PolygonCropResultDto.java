package uk.co.fivium.gisframework.grpc;

import uk.co.fivium.grpc.gis.IntersectionStatus;

public record PolygonCropResultDto(
    IntersectionStatus status,
    String esriJsonCroppedPolygon
) {
}
