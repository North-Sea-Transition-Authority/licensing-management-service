import { ArcGisServiceHandlers } from '../../generated/uk/co/fivium/grpc/gis/ArcGisService';
import { logger } from '../config/logger';
import { toGrpcInternalError } from './grpc-error';
import { validatePolygonReconstructionFromPolylines } from '../geometric-operators/validate-polygon-reconstruction-from-polylines';
import { esriJsonToPolyline } from '../util/esrijson-util';

/**
 * Validates that a polygon can be reconstructed from a list of ordered polylines.
 * @param call GRPC call with a list of polylines and the original polygon.
 * @param callback Response callback. Contains a boolean indicating whether the polygon can be reconstructed.
 */
export const validatePolygonReconstructionFromPolylinesHandler: ArcGisServiceHandlers['validatePolygonReconstructionFromPolylines'] =
  (call, callback) => {
    try {
      const orderedLines = call.request.lines.map((line) => ({
        polyline: esriJsonToPolyline(line.esriJsonPolyline),
        ringNumber: line.ringNumber,
        connectionOrder: line.connectionOrder,
      }));
      const isValid = validatePolygonReconstructionFromPolylines(orderedLines, call.request.originalPolygonEsriJson);
      callback(null, { isValid: isValid });
    } catch (error) {
      logger.error({ error: error }, 'Error validating polygon reconstruction from polylines');
      callback(toGrpcInternalError(error), null);
    }
  };
