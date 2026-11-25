package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayloadService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.ChronologicalPositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;

@Service
public class CorrectedTimelineService {

  private final LicencePositionStagedPayloadService licencePositionStagedPayloadService;
  private final ChronologicalPositionService chronologicalPositionService;

  public CorrectedTimelineService(
      LicencePositionStagedPayloadService licencePositionStagedPayloadService,
      ChronologicalPositionService chronologicalPositionService
  ) {
    this.licencePositionStagedPayloadService = licencePositionStagedPayloadService;
    this.chronologicalPositionService = chronologicalPositionService;
  }

  public CorrectedTimeline getCorrectedTimeline(LicenceCorrection licenceCorrection) {
    var positionStagedPayloads = licencePositionStagedPayloadService.getPositionStagedPayloads(licenceCorrection);
    var removedPositionIds = LicencePositionStagedPayloadService.getRemovedPositionIds(positionStagedPayloads);
    var displayedPositions = chronologicalPositionService.getCorrectedChronologicalPositions(
        licenceCorrection,
        positionStagedPayloads,
        removedPositionIds
    );

    return new CorrectedTimeline(
        licenceCorrection,
        positionStagedPayloads,
        displayedPositions,
        LicencePositionStateResolver.resolve(displayedPositions, removedPositionIds)
    );
  }
}