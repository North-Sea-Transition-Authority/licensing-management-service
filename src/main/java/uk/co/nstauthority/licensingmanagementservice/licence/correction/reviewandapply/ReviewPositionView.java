package uk.co.nstauthority.licensingmanagementservice.licence.correction.reviewandapply;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;

public record ReviewPositionView(
    UUID positionId,
    String positionName,
    String reference,
    CorrectionMarker marker,
    List<ReviewChangeView> changes
) {

  static ReviewPositionView from(
      ChronologicalPosition position,
      @Nullable CorrectionReviewService.PositionDetails details,
      ReviewPositionContext correctedContext,
      ChangeEdits changeEdits
  ) {
    return new ReviewPositionView(
        position.id(),
        position.positionName(),
        details != null ? details.correctionReference() : position.reference(),
        details != null ? CorrectionMarker.forPosition(details.changeType()) : CorrectionMarker.POSITION_CORRECTED,
        ReviewChangeView.forPosition(position, correctedContext, changeEdits)
    );
  }
}