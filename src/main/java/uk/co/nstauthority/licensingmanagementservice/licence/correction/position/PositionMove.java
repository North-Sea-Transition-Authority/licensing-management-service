package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.util.UUID;

public record PositionMove(PositionMoveDirection direction, UUID targetId) {

  private static final String SEPARATOR = ":";

  public String toFormValue() {
    return direction.name() + SEPARATOR + targetId;
  }

  public static PositionMove fromFormValue(String formValue) {
    var parts = formValue.split(SEPARATOR, 2);
    return new PositionMove(PositionMoveDirection.valueOf(parts[0]), UUID.fromString(parts[1]));
  }
}