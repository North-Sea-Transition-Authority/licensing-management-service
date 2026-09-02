package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public sealed interface LicencePositionChangeView permits
    AdministratorChangeView,
    SetEquityChangeView,
    TransferEquityChangeView,
    PartialSurrenderChangeView,
    SubareaChangeView {

  @Nullable
  String changeType();

  ChangeViewUrls urls();

  String type();

  default LicencePositionChangeView merge(LicencePositionChangeView other) {
    throw new UnsupportedOperationException(
        "merge is not supported for %s".formatted(getClass().getSimpleName()));
  }
}
