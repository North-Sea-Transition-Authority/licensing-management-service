import { esriJsonToPolygon, esriJsonToPolyline } from '../util/esrijson-util';
import { splitPolygon } from '../geometric-operators/split-operator';
import { ArcGisServiceHandlers } from '../../generated/uk/co/fivium/grpc/gis/ArcGisService';

/**
 * Split a polygon with a cutter line.
 * @param call GRPC call with a target polygon and a cutter line.
 * @param callback Response callback. Contains output polygons resulting from the split, returned as Esri JSON strings.
 */
export const splitPolygonHandler: ArcGisServiceHandlers['splitPolygon'] = (call, callback) => {
  try {
    const target = esriJsonToPolygon(call.request.esriJsonPolygonTarget);
    const cutterLine = esriJsonToPolyline(call.request.esriJsonLineCutter);
    const polygons = splitPolygon(target, cutterLine);

    const response: string[] = (polygons || []).map((poly) => JSON.stringify(poly.toJSON()));

    callback(null, { outputPolygonEsriJsons: response });
  } catch (error) {
    console.error({ error: error }, 'Error splitting polygon');
    callback(error, null);
  }
};
