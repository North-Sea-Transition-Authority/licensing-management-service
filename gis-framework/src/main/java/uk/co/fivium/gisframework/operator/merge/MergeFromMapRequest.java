package uk.co.fivium.gisframework.operator.merge;

import java.util.List;
import java.util.UUID;

public record MergeFromMapRequest(List<UUID> featureIds, UUID commandJourneyId) {
}
