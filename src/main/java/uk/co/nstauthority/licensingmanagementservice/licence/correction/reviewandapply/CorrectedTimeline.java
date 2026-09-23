package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;

public record CorrectedTimeline(
    LicenceCorrection licenceCorrection,
    List<LicencePositionCorrection> positionCorrections,
    List<ChronologicalPosition> displayedPositions,
    ResolvedStates resolvedStates
) {

  public List<ChronologicalPosition> positionsToApply() {
    var removedPositionIds = LicencePositionCorrectionService.getRemovedPositionIds(positionCorrections);
    return displayedPositions.stream()
        .filter(position -> !removedPositionIds.contains(position.id()))
        .toList();
  }

  public boolean isCarbonStorage() {
    return licenceCorrection.getLicence().getType() == LicenceType.CARBON_STORAGE;
  }
}