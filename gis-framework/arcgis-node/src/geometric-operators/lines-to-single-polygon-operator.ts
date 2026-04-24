import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as linesToPolygonsOperator from '@arcgis/core/geometry/operators/linesToPolygonsOperator.js';
import * as simplifyOperator from '@arcgis/core/geometry/operators/simplifyOperator.js';
import { logger } from '../config/logger';

/**
 * Converts an array of {@link Polyline} geometries into a single simplified polygon.
 *
 * The polylines are converted into polygons using the {@link linesToPolygonsOperator}.
 * https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/linesToPolygonsOperator/
 *
 *
 * Only the first resulting polygon is retained as any additional polygons represent interior holes and are already
 * incorporated into the first polygon.
 *
 * The polygon is also simplified using the {@link simplifyOperator} to ensure it is valid and to remove any unnecessary vertices.
 * https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/simplifyOperator/
 *
 * @param polylines - An array of {@link Polyline} geometries to convert into a polygon.
 * @param wkid - The Well-Known ID (WKID) of the spatial reference to assign to the output polygon.
 * @returns A simplified {@link Polygon} constructed from the provided polylines.
 */
export function linesToSinglePolygon(polylines: Polyline[], wkid: number): Polygon {
  const polygons = linesToPolygonsOperator.executeMany(polylines);

  const polygon = polygons[0];
  polygon.spatialReference = { wkid: wkid };

  logger.debug(`Built ${polygons.length} polygons`);

  return simplifyOperator.execute(polygon) as Polygon;
}

/**
 * This is wrapper of {@link linesToSinglePolygon} which allows you to pass in an array of EsriJSON polylines in string format.
 *
 * @param esriJsonLineStrings - An array of EsriJSON polylines in string format.
 * @param wkid - The Well-Known ID (WKID) of the spatial reference to assign to the output polygon.
 * @returns A simplified {@link Polygon} constructed from the provided polylines.
 */
export function lineStringsToSinglePolygon(esriJsonLineStrings: string[], wkid: number): Polygon {
  const polylines: Polyline[] = [];

  esriJsonLineStrings.forEach((lineString) => {
    polylines.push(Polyline.fromJSON(JSON.parse(lineString)));
  });

  return linesToSinglePolygon(polylines, wkid);
}
