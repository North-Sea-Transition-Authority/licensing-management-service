package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public record LicenseeChangeView(
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return "";
  }
}
