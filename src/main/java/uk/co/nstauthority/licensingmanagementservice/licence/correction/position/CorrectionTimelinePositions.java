package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.payloads.UpdateLicencePositionPayload;

record CorrectionTimelinePositions(
    List<LicencePosition> executedPositions,
    List<LicencePositionStagedPayload> positionStagedPayloads,
    Set<UUID> removedPositionIds,
    Map<UUID, UpdateLicencePositionPayload> updatedPositionPayloadsByTargetId,
    List<ChronologicalPosition> correctionTimelinePositions
) {
}
