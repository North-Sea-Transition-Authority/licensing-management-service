package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public sealed interface LicencePositionChangeView permits AdministratorChangeView, SetEquityChangeView {

  @Nullable
  String changeType();

  default LicencePositionChangeView merge(LicencePositionChangeView other) {
    throw new UnsupportedOperationException(
        "merge is not supported for %s".formatted(getClass().getSimpleName()));
  }
}
