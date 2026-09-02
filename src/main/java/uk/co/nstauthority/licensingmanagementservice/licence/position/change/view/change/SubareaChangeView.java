package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record SubareaChangeView(
    String featureName,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.SUBAREA;
  }
}
