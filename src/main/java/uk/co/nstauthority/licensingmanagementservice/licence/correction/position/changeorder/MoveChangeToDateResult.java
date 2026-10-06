package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeorder;

import jakarta.annotation.Nullable;
import java.util.UUID;

public record MoveChangeToDateResult(Outcome outcome, @Nullable UUID positionId) {

  public enum Outcome {
    MOVED_TO_EXISTING_POSITION,
    MOVED_TO_NEW_POSITION,
    NEEDS_POSITION_ORDER
  }
}