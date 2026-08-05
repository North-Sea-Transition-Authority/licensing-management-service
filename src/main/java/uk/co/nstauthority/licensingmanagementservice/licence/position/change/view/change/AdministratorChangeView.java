package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;

public record AdministratorChangeView(
    @Nullable String withdrawingOrganisationName,
    String joiningOrganisationName,
    String changeId,
    String changeType,
    @Nullable String correctUrl,
    @Nullable String removeUrl,
    @Nullable String undoUrl
) implements LicencePositionChangeView {
}
