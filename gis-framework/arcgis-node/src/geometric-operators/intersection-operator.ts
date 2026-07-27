import type Polygon from "@arcgis/core/geometry/Polygon.js";
import * as intersectionOperator from "@arcgis/core/geometry/operators/intersectionOperator.js";
import { esriJsonToPolygon } from "../util/esrijson-util";

/**
 * Intersects two polygons, returning only the portion where they overlap.
 * @param polygon1EsriJson The first polygon to intersect.
 * @param polygon2EsriJson The second polygon to intersect.
 * @return The overlapping portion of the two polygons, or null if they do not overlap at all.
 */
export function intersectPolygons(polygon1EsriJson: string, polygon2EsriJson: string): Polygon | null {
  const polygon1 = esriJsonToPolygon(polygon1EsriJson);
  const polygon2 = esriJsonToPolygon(polygon2EsriJson);

  return (intersectionOperator.execute(polygon1, polygon2) as Polygon | null | undefined) ?? null;
}
