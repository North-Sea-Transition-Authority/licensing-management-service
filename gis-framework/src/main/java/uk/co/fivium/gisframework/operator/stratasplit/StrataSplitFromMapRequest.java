package uk.co.fivium.gisframework.operator.stratasplit;

import java.util.List;
import java.util.UUID;

public record StrataSplitFromMapRequest(
    List<UUID> featureIds,
    UUID commandJourneyId,
    Long depth
) {
}
