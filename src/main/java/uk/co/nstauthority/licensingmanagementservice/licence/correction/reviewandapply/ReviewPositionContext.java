package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

record ReviewPositionContext(
    List<ChronologicalPosition> positions,
    ResolvedStates resolvedStates,
    Map<Integer, String> organisationNames,
    Map<UUID, String> featureNames
) {
}
