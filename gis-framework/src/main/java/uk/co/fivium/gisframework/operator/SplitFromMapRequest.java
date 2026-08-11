package uk.co.fivium.gisframework.operator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A request to split a journey's active features using a cutter line drawn on the map.
 *
 * @param cutterLineOriginalSrsCoordinates the cutter line as a list of 2-point segments (each an [x, y] coordinate pair),
 *                               in the same coordinate system as the journey's features.
 * @param commandJourneyId       the journey whose active features should be split.
 */
public record SplitFromMapRequest(List<List<List<BigDecimal>>> cutterLineOriginalSrsCoordinates,
                                  UUID commandJourneyId) {
}
