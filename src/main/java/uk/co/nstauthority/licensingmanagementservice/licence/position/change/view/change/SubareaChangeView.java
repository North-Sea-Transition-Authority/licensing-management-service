package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public record SubareaChangeView(
    String featureName,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {
}
