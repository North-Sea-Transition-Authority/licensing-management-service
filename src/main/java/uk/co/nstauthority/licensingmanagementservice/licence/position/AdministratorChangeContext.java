package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.annotation.Nullable;

public record AdministratorChangeContext(
    @Nullable Integer currentAdministratorId,
    @Nullable Integer previousAdministratorId,
    String currentAdministratorName,
    String previousAdministratorName
) {
}
