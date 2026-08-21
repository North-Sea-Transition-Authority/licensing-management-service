package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;

/**
 * Scopes the summary sections to a partial surrender. Sections are typed against this context rather than
 * {@link LicencePositionCorrection} directly so that summaries for other change types staged on the same entity
 * cannot be injected into the partial surrender summary.
 */
public sealed interface PartialSurrenderSummaryContext {

  record Staged(LicencePositionCorrection licencePositionCorrection) implements PartialSurrenderSummaryContext {
  }

  record LiveChange(LicenceCorrection correction, LicencePosition licencePosition, String changeId)
      implements PartialSurrenderSummaryContext {
  }
}
