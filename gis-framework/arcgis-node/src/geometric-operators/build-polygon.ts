import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as linesToPolygonsOperator from '@arcgis/core/geometry/operators/linesToPolygonsOperator.js';
import * as simplifyOperator from '@arcgis/core/geometry/operators/simplifyOperator.js';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import { logger } from '../config/logger';

/**
 * Builds a polygon from a list of polylines.
 * @param polylines List of polylines to build the polygon from.
 * @param coordinateSystemWkid Spatial reference ID (WKID) of the coordinate system of the polylines.
 * @return The built polygon, or undefined if no polygon could be built.
 */
export function buildPolygon(polylines: Polyline[], coordinateSystemWkid: number): Polygon | undefined {
  const polygons = linesToPolygonsOperator.executeMany(polylines);

  if (polygons.length === 0) {
    logger.error({ polylines: polylines }, 'No polygons could be built from the provided polylines');
    return undefined;
  }

  // We only want the first polygon, if there is more than one polygon, then they will be the holes of a polygon with holes
  // and will already be included in the first polygon.
  const polygon = polygons[0];
  polygon.spatialReference = { wkid: coordinateSystemWkid };

  return simplifyOperator.execute(polygon) as Polygon;
}
