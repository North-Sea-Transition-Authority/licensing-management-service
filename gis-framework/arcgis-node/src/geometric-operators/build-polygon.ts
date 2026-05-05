import type { ArcGisServiceHandlers } from '../../generated/arcgisjs/ArcGisService.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as linesToPolygonsOperator from '@arcgis/core/geometry/operators/linesToPolygonsOperator.js';
import * as simplifyOperator from '@arcgis/core/geometry/operators/simplifyOperator.js';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import { logger } from '../config/logger';
import { esriJsonToPolyline } from '../util/esrijson-util';

/**
 * Builds a polygon from a list of polyline EsriJSONs.
 * @param call GRPC call with a list of polylines as EsriJSON strings and a spatial reference ID (WKID).
 * @param callback Response callback. Contains the resulting polygon as an EsriJSON string.
 */
export const buildPolygon: ArcGisServiceHandlers['buildPolygon'] = (call, callback) => {
  const polylines: Polyline[] = call.request.esriJsonPolylines.map((lineJson: string) => esriJsonToPolyline(lineJson));

  const polygons = linesToPolygonsOperator.executeMany(polylines);

  if (polygons.length === 0) {
    logger.error({ polylines: call.request.esriJsonPolylines }, 'No polygons could be built from the provided polylines');
    callback(new Error('No polygons could be built from the provided polylines'), null);
    return;
  }

  // We only want the first polygon, if there is more than one polygon, then they will be the holes of a polygon with holes
  // and will already be included in the first polygon.
  const polygon = polygons[0];
  polygon.spatialReference = { wkid: call.request.coordinateSystemWkid };

  const simplifiedPolygon = simplifyOperator.execute(polygon) as Polygon;

  callback(null, { polygonEsriJson: JSON.stringify(simplifiedPolygon.toJSON()) });
};
