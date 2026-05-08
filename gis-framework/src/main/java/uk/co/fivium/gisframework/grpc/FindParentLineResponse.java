package uk.co.fivium.gisframework.grpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FindParentLineResponse(Map<String, UUID> polylineToParentLineId,
                                     List<String> orphanLines) {
}
