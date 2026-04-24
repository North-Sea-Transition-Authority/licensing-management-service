package uk.co.fivium.gisframework.migration;

import java.util.Map;

public record MigrationResponseDto(
    Map<Integer, String> oracleSsidToEsriJsonLineString,
    Double area
) {
}
