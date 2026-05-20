import type Polygon from "@arcgis/core/geometry/Polygon.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import { logger } from "../config/logger";

/**
 * Perform a topological union (dissolve) operation on a list of polygons using the unionOperator.
 * https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/unionOperator/
 *
 * If there is only one polygon in the list, it will return itself.
 *
 * @param polygons A list of Polygons to be unioned together.
 * @returns A single Polygon that is the result of the union operation.
 */
export function unionPolygonsOperator(polygons: Polygon[]): Polygon {
  if (polygons.length === 1) {
    logger.debug("Received single polygon, nothing to union");
    return polygons[0];
  }

  return unionOperator.executeMany(polygons) as Polygon;
}
