package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import jakarta.annotation.Nullable;

public record LicenseeView(
    String organisationName,
    @Nullable OrganisationMarker marker
) {
}