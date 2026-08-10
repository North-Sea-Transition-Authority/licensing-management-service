import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";

export interface CoordinatePair {
  x: number,
  y: number,
}

/**
 * Builds an EsriJSON polyline from an ordered line of coordinates.
 * @param coordinates the ordered coordinates the polyline should pass through.
 * @param srsWkid the coordinate system the coordinates are expressed in.
 * @returns EsriJSON of the built polyline as a string.
 */
export function coordinatesToPolyline(coordinates: CoordinatePair[], srsWkid: number): string {
  const coordinatesPath = coordinates.map(coordinate => [coordinate.x, coordinate.y]);
  const polyline = new Polyline({
    paths: [coordinatesPath],
    spatialReference: new SpatialReference({ wkid: srsWkid }),
  });
  const polylineSimplified = simplifyOperator.execute(polyline) as Polyline;
  return JSON.stringify(polylineSimplified.toJSON());
}
