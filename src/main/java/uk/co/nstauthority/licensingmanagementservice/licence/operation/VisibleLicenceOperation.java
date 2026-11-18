package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.action.ChangeActionHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeViewContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.ChangeViewUrls;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeView;

/**
 * An operation that is shown on a position as a change view.
 *
 * <p>The block, subarea creation and subarea end operations the PEARS migration produces are {@link HiddenLicenceOperation}s
 * instead: they are carried across so the spatial timeline is right, and nothing has been designed for showing them on a
 * position yet.
 */
public non-sealed interface VisibleLicenceOperation extends LicenceOperation {

  /**
   * The change this operation makes, in lower case as it reads mid-sentence, e.g. "licensee change".
   */
  default String changeName() {
    return displayName().toLowerCase();
  }

  /**
   * Whether a position holds at most one change carrying this operation. Removing a live change of such an operation
   * also drops any change of it the correction has staged against the position.
   */
  default boolean atMostOnePerPosition() {
    return false;
  }

  /**
   * The handler for removing, undoing or reinstating a change this operation is shown as, or null when such a change
   * can't be removed, undone or reinstated.
   */
  @Nullable
  Class<? extends ChangeActionHandler> changeActionHandlerType();

  /**
   * The view this operation is shown as.
   *
   * @param previousState the state of the position before the change this operation belongs to
   * @param currentPositionDate the date of the position being viewed, if it has one
   * @param urls the actions the view offers, built by the caller
   */
  LicencePositionChangeView getChangeView(
      PositionChange change,
      LicencePositionState previousState,
      @Nullable LocalDate currentPositionDate,
      LicencePositionChangeViewContext context,
      ChangeViewUrls urls
  );

  @JsonIgnore
  OperationRoutes getOperationUrls();

}
