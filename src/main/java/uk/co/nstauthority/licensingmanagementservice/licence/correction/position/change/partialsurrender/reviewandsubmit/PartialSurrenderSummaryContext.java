package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import jakarta.annotation.Nullable;
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

  /**
   * A partial surrender staged on a position correction.
   *
   * @param licencePositionCorrection The position correction the surrender is staged on.
   * @param surrender                 The partial surrender being summarised.
   * @param stateBefore               The state of the position going into the surrender.
   */
  record Staged(
      LicencePositionCorrection licencePositionCorrection,
      PartialSurrenderOperation surrender,
      LicencePositionState stateBefore
  ) implements PartialSurrenderSummaryContext {
  }

  /**
   * A partial surrender on a live position change.
   *
   * @param correction      The correction the change is being corrected in, or null when the summary is read-only
   *                        (i.e. viewed outside a correction).
   * @param licencePosition The position holding the change.
   * @param changeId        The id of the change.
   * @param surrender       The partial surrender being summarised.
   * @param stateBefore     The state of the position going into the change.
   */
  record LiveChange(
      @Nullable LicenceCorrection correction,
      LicencePosition licencePosition,
      String changeId,
      PartialSurrenderOperation surrender,
      LicencePositionState stateBefore
  ) implements PartialSurrenderSummaryContext {
  }
}
