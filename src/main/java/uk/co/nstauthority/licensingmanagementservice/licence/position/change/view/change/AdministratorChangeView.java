package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record AdministratorChangeView(
    @Nullable String withdrawingOrganisationName,
    String joiningOrganisationName,
    String changeId,
    String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.LICENCE_ADMINISTRATOR;
  }
}
