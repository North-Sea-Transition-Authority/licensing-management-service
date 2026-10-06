package uk.co.fivium.gisframework.operator;

import jakarta.annotation.Nullable;
import java.util.UUID;
import uk.co.fivium.grpc.gis.IntersectionStatus;

/**
 * The result of cropping a feature to a boundary.
 *
 * @param inputFeatureId   The id of the feature that was cropped, which is left unchanged.
 * @param status           Whether the feature lies fully outside, fully inside or across the boundary.
 * @param croppedFeatureId The id of the new feature holding the part of the input feature inside the boundary. Only
 *                         present when the status is CROPPED.
 */
public record CropResultDto(
    UUID inputFeatureId,
    IntersectionStatus status,
    @Nullable UUID croppedFeatureId
) {
}
