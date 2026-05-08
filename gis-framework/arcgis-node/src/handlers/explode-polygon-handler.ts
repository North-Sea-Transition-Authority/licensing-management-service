import { explodePolygon } from '../geometric-operators/explode-polygon';
import { ArcGisServiceHandlers } from '../../generated/uk/co/fivium/grpc/gis/ArcGisService';
import { esriJsonToPolygon } from '../util/esrijson-util';
import { logger } from '../config/logger';
import { toGrpcInternalError } from './grpc-error';

/**
 * Explode a polygon into its individual polylines.
 * Each polyline is represented as a separate polyline with two points (start and end).
 * @param call GRPC call with a polygon to explode.
 * @param callback Response callback. Contains the exploded polylines, returned as Esri JSON strings.
 */
export const explodePolygonHandler: ArcGisServiceHandlers['explodePolygon'] = (call, callback) => {
  try {
    const polygon = esriJsonToPolygon(call.request.esriJsonPolygon);
    const polylines = explodePolygon(polygon);
    const esriJsonLines = polylines.map((s) => JSON.stringify(s.toJSON()));

    callback(null, { esriJsonLines: esriJsonLines });
  } catch (error) {
    logger.error({ error: error }, 'Error exploding polygon');
    callback(toGrpcInternalError(error), null);
  }
};
