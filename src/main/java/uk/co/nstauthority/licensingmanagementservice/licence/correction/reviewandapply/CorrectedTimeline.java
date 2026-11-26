package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.staged.LicencePositionStagedPayload;

public record CorrectedTimeline(
    LicenceCorrection licenceCorrection,
    List<LicencePositionStagedPayload> positionStagedPayloads,
    List<ChronologicalPosition> displayedPositions,
    ResolvedStates resolvedStates
) {

  public List<ChronologicalPosition> positionsToApply() {
    var removedPositionIds = LicencePositionStagedPayloadService.getRemovedPositionIds(positionStagedPayloads);
    return displayedPositions.stream()
        .filter(position -> !removedPositionIds.contains(position.id()))
        .toList();
  }

  public LicenceType licenceType() {
    return licenceCorrection.getLicence().getType();
  }
}