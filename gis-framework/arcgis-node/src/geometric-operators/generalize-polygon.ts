import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon.js";

// Tolerance for generalising line geometries in degrees (~1 mm on ED50).
export const GENERALIZE_TOLERANCE_DEGREES = 0.00000001;

export function generalizePolygon(esriPolygon: string): string {
  const polygon = Polygon.fromJSON(JSON.parse(esriPolygon));

  // remove vertices that are on straight lines.
  const result = generalizeOperator.execute(polygon, GENERALIZE_TOLERANCE_DEGREES) as Polygon;

  return JSON.stringify(result.toJSON());
}
