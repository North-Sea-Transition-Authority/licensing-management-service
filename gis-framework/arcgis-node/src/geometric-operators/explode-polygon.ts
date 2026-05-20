import type Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";

/**
 * Explode a polygon into its individual polylines.
 * Each polyline is represented as a separate polyline with two points (start and end).
 * @param polygon The polygon to explode.
 * @return An array of polylines, each representing a single segment of the original polygon.
 */
export function explodePolygon(polygon: Polygon): Polyline[] {
  const polylines: Polyline[] = [];

  polygon.rings.forEach((ring) => {
    for (let i = 0; i < ring.length - 1; i++) {
      const startPoint = ring[i];
      const endPoint = ring[i + 1];
      const segment = new Polyline({
        paths: [[startPoint, endPoint]],
        spatialReference: polygon.spatialReference,
      });

      polylines.push(segment);
    }
  });

  return polylines;
}
