package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.Map;
import java.util.UUID;

public record LicencePositionChangeViewContext(
    Map<Integer, String> organisationNames,
    Map<UUID, String> featureNames
) {
}
