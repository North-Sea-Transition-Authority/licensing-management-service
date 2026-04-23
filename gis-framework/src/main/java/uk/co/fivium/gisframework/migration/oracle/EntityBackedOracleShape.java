package uk.co.fivium.gisframework.migration.oracle;

import java.util.List;
import java.util.Map;

/**
 * This record provides a way to link various entities that make up an oracle shape together.
 * @param shape The main oracle shape the other fields relate to.
 * @param polygonToBoundary A mapping of polygons to a list of their boundaries.
 * @param boundaryToLine A mapping of boundaries to a list of their lines.
 */
public record EntityBackedOracleShape(
    OracleShape shape,
    Map<OracleShapePolygon, List<OraclePolygonBoundary>> polygonToBoundary,
    Map<OraclePolygonBoundary, List<OracleBoundaryLine>> boundaryToLine
) {
}
