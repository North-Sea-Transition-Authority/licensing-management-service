import { ArcGisServiceHandlers } from '../../generated/uk/co/fivium/grpc/gis/ArcGisService';
import { getLineStartAndEndPoints, PolylineWithId } from '../geometric-operators/get-line-start-and-end-points';
import { esriJsonToPolyline } from '../util/esrijson-util';
import { logger } from '../config/logger';
import { toGrpcInternalError } from './grpc-error';

export const getLineStartAndEndPointsHandler: ArcGisServiceHandlers['getLineStartAndEndPoints'] = (call, callback) => {
  try {
    const polylinesWithId: PolylineWithId[] = call.request.lines.map((line) => {
      return {
        id: line.id,
        polyline: esriJsonToPolyline(line.polyLineEsriJson),
      };
    });

    const result = getLineStartAndEndPoints(polylinesWithId);
    callback(null, {
      lines: result,
    });
  } catch (error) {
    logger.error({ error: error }, 'Error getting line start and end points');
    callback(toGrpcInternalError(error), null);
  }
};
