package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view;

import jakarta.annotation.Nullable;

public record AdministratorChangeView(
    @Nullable String withdrawingOrganisationName,
    String joiningOrganisationName
) implements LicencePositionChangeView {
}
