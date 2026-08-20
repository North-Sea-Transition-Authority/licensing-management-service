package uk.co.nstauthority.licensingmanagementservice.licence.overview;

import jakarta.annotation.Nullable;

public record LicenceOverviewView(
    String licenceReference,
    String caption,
    @Nullable String csRegisterUrl
) {
}
