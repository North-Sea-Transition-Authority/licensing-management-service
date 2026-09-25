import * as multiPartToSinglePartOperator from "@arcgis/core/geometry/operators/multiPartToSinglePartOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon.js";

/**
 * Split a (possibly multipart) polygon into single-part polygons.
 * Each clockwise outer ring plus its anti-clockwise holes
 * becomes one single-part polygon.
 * @param inputPolygon EsriJSON of the polygon to split.
 * @return EsriJSON of each disjoint single-part polygon.
 */
export function multiPartToSinglePart(inputPolygon: string): string[] {
  const polygon = Polygon.fromJSON(JSON.parse(inputPolygon));
  const parts = multiPartToSinglePartOperator.executeMany([polygon]) as Polygon[];
  return parts.map(part => JSON.stringify(part.toJSON()));
}
