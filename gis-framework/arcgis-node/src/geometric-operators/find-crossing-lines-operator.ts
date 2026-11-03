import type { LineWithId__Output } from "../../generated/uk/co/fivium/grpc/gis/LineWithId";
import * as crossesOperator from "@arcgis/core/geometry/operators/crossesOperator.js";
import { esriJsonToPolygon, esriJsonToPolyline } from "../util/esrijson-util";

/**
 * Finds the lines that cross any of the given polygons, in two dimensions. A line crosses a polygon when part of it is
 * inside the polygon and part of it is outside. A line wholly inside or outside a polygon, or that only runs along or
 * touches its boundary, does not cross it.
 * @param esriJsonPolygons The polygons to check the lines against.
 * @param lines The lines to check.
 * @return The ids of the lines that cross any of the polygons, in the order they were given.
 */
export function findCrossingLines(
  esriJsonPolygons: string[],
  lines: LineWithId__Output[],
): string[] {
  const polygons = esriJsonPolygons.map(esriJsonToPolygon);

  return lines
    .filter((line) => {
      const polyline = esriJsonToPolyline(line.polyLineEsriJson);
      return polygons.some(polygon => crossesOperator.execute(polyline, polygon));
    })
    .map(line => line.id);
}
