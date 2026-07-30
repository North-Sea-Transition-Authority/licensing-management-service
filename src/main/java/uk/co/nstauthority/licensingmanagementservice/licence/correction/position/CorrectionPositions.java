package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

record CorrectionPositions(
    List<LicencePosition> executedPositions,
    List<LicencePositionCorrection> addCorrections,
    List<LicencePositionCorrection> updateCorrections,
    Set<UUID> removedPositionIds
) {
}