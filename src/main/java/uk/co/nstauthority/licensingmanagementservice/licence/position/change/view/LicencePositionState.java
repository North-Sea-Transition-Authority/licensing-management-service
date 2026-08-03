package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;

public record LicencePositionState(
    @Nullable Integer administratorId
) {

  public static final LicencePositionState EMPTY = new LicencePositionState(null);

  public LicencePositionState withAdministratorId(Integer administratorId) {
    return new LicencePositionState(administratorId);
  }
}
