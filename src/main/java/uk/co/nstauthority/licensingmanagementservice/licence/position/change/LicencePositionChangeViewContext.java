package uk.co.nstauthority.licensingmanagementservice.licence.position.change;

import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;

public record LicencePositionChangeViewContext(
    Map<Integer, String> organisationNames,
    Map<UUID, FeatureSummary> featuresById
) {
}
