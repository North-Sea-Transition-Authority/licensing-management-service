package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

public record CorrectionPositions(
    List<LicencePosition> executedPositions,
    List<LicencePositionStagedPayload> addStagedPayloads,
    List<LicencePositionStagedPayload> updateStagedPayloads,
    Set<UUID> removedPositionIds
) {
}