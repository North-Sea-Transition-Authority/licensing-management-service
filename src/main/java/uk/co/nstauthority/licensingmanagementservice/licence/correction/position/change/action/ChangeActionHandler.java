package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action;

import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionStagedPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;

/**
 * The parts of removing, undoing or reinstating a change that differ by the kind of change it is and need services to
 * work out. Everything else - routing, page title, buttons, banners and the staging itself - is shared by
 * {@link ChangeActionController} and {@link ChangeActionService}, and what an operation can say about itself lives on
 * the operation.
 *
 * <p>Each {@link uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation} names its
 * handler through {@code changeActionHandlerType()}.
 */
public interface ChangeActionHandler {

  /**
   * The confirmation page for removing a live change, summarising the change. The controller adds
   * {@code pageTitle}, {@code primaryButton} and {@code cancelUrl} to it.
   */
  ModelAndView executedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePosition licencePosition,
      LicencePositionChange liveChange
  );

  /**
   * The confirmation page for undoing or reinstating a change this correction has staged, summarising the change. The
   * controller adds {@code pageTitle}, {@code primaryButton} and {@code cancelUrl} to it.
   */
  ModelAndView stagedChangeConfirmation(
      LicenceCorrection licenceCorrection,
      LicencePositionStagedPayload positionStagedPayload,
      String changeId,
      ChangeAction action
  );

  /**
   * Runs before a staged change is dropped by an undo or reinstate, within the same transaction.
   */
  default void beforeDroppingStagedChange(
      LicencePositionStagedPayload positionStagedPayload,
      LicencePositionChangeType stagedChange
  ) {
  }
}
