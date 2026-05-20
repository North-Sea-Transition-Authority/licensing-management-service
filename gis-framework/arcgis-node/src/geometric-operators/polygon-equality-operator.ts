import type Polygon from "@arcgis/core/geometry/Polygon.js";
import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator.js";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";
import { esriJsonToPolygon } from "../util/esrijson-util";

/**
 * Verifies that two polygons are topologically equal.
 * @param esriJsonPolygon1
 * @param esriJsonPolygon2
 * @return true if the polygons are topologically equal, false otherwise.
 */
export function polygonsAreTopologicallyEqual(esriJsonPolygon1: string, esriJsonPolygon2: string): boolean {
  const polygon1 = esriJsonToPolygon(esriJsonPolygon1);
  const simplePolygon1 = simplifyOperator.execute(polygon1) as Polygon;
  const polygon2 = esriJsonToPolygon(esriJsonPolygon2);
  const simplePolygon2 = simplifyOperator.execute(polygon2) as Polygon;

  return equalsOperator.execute(simplePolygon1, simplePolygon2);
}
