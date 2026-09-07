import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon.js";

export function mergePolygons(inputPolygon1: string, inputPolygon2: string): string {
  const polygon1 = Polygon.fromJSON(JSON.parse(inputPolygon1));
  const polygon2 = Polygon.fromJSON(JSON.parse(inputPolygon2));

  const result = unionOperator.execute(polygon1, polygon2) as Polygon;

  return JSON.stringify(result.toJSON());
}
