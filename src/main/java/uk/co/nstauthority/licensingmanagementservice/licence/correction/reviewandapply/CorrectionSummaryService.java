package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;

@Service
public class CorrectionSummaryService {

  private final CorrectionReviewService correctionReviewService;
  private final CorrectedTimelineService correctedTimelineService;

  public CorrectionSummaryService(
      CorrectionReviewService correctionReviewService,
      CorrectedTimelineService correctedTimelineService
  ) {
    this.correctionReviewService = correctionReviewService;
    this.correctedTimelineService = correctedTimelineService;
  }

  public List<ReviewPositionView> getSummaryPositions(LicenceCorrection licenceCorrection) {
    if (licenceCorrection.isComplete()) {
      return correctionReviewService.getAppliedPositions(licenceCorrection);
    }

    return correctionReviewService.getReviewPositions(
        correctedTimelineService.getCorrectedTimeline(licenceCorrection),
        List.of()
    );
  }
}
