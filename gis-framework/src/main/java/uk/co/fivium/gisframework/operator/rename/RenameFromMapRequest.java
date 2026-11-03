package uk.co.fivium.gisframework.operator.rename;

import java.util.List;
import java.util.UUID;

public record RenameFromMapRequest(
    UUID commandJourneyId,
    List<JsonFeatureRename> featureRenames
) {}