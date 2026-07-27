package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public sealed interface LicencePositionChangeView permits AdministratorChangeView {

  @Nullable
  String changeType();

}
