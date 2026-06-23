package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.util.UUID;

public record LicencePositionTimelineView(
    UUID positionId,
    String url,
    String regulatorReference,
    String formattedPositionDate
) {
}
