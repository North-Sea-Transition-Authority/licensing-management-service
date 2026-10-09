package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import jakarta.annotation.Nullable;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;

/**
 * A change being linked to from the position it is viewed on within a correction.
 */
public record ChangeUrlTarget(PositionChangeUrlContext context, PositionChange change) {

  /**
   * What this correction has done to the change so far.
   */
  public enum State {
    LIVE,
    ADDED,
    OPERATIONS_UPDATED,
    ORDER_UPDATED,
    REMOVED
  }

  public State state() {
    var changeType = change.changeType();
    if (changeType == null) {
      return State.LIVE;
    }
    return switch (changeType) {
      case LicencePositionChangeType.ADD_CHANGE -> State.ADDED;
      case LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS -> State.OPERATIONS_UPDATED;
      case LicencePositionChangeType.UPDATE_CHANGE_ORDER -> State.ORDER_UPDATED;
      case LicencePositionChangeType.REMOVE_CHANGE -> State.REMOVED;
      default -> throw new IllegalStateException("Unknown change type %s".formatted(changeType));
    };
  }

  public boolean addedPosition() {
    return context.addedPosition();
  }

  public LicenceCorrection correction() {
    return context.correction();
  }

  @Nullable
  public LicencePosition licencePosition() {
    return context.licencePosition();
  }

  @Nullable
  public LicencePositionCorrection positionCorrection() {
    return context.positionCorrection();
  }

  public String changeId() {
    return change.changeId();
  }

  public LicencePositionChange changeEntity() {
    return new LicencePositionChange(UUID.fromString(change.changeId()));
  }
}
