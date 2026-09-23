package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionStateResolver;

@Service
public class CorrectedTimelineService {

  private final LicencePositionCorrectionService licencePositionCorrectionService;
  private final LicencePositionViewService licencePositionViewService;

  public CorrectedTimelineService(
      LicencePositionCorrectionService licencePositionCorrectionService,
      LicencePositionViewService licencePositionViewService
  ) {
    this.licencePositionCorrectionService = licencePositionCorrectionService;
    this.licencePositionViewService = licencePositionViewService;
  }

  public CorrectedTimeline getCorrectedTimeline(LicenceCorrection licenceCorrection) {
    var positionCorrections = licencePositionCorrectionService.getPositionCorrections(licenceCorrection);
    var removedPositionIds = LicencePositionCorrectionService.getRemovedPositionIds(positionCorrections);
    var displayedPositions = licencePositionViewService.getCorrectedChronologicalPositions(
        licenceCorrection, positionCorrections, removedPositionIds);

    return new CorrectedTimeline(
        licenceCorrection,
        positionCorrections,
        displayedPositions,
        LicencePositionStateResolver.resolve(displayedPositions, removedPositionIds)
    );
  }
}