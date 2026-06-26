package uk.co.fivium.gisframework.feature;

import java.util.List;

public record JsonFeatureOutlineNodes(
    String featureId,
    List<JsonOutlineNode> nodes
) {
}
