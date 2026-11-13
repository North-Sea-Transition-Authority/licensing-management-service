package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

/**
 * Scopes the summary sections to a partial surrender. Sections are typed against this context rather than
 * {@link LicencePositionCorrection} directly so that summaries for other change types staged on the same entity
 * cannot be injected into the partial surrender summary.
 */
public sealed interface PartialSurrenderSummaryContext {

  PartialSurrenderOperation surrender();

  LicencePositionState stateBefore();

  record Staged(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation surrender,
      LicencePositionState stateBefore
  ) implements PartialSurrenderSummaryContext {
  }

  record LiveChange(
      LicenceCorrection correction,
      LicencePosition licencePosition,
      String changeId,
      PartialSurrenderOperation surrender,
      LicencePositionState stateBefore
  ) implements PartialSurrenderSummaryContext {
  }
}
