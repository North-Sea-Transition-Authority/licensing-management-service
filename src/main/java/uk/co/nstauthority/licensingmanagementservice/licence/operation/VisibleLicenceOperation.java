package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
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
