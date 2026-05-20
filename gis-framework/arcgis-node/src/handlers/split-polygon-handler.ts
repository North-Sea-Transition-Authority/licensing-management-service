import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import { logger } from "../config/logger";
import { splitPolygon } from "../geometric-operators/split-operator";
import { esriJsonToPolygon, esriJsonToPolyline } from "../util/esrijson-util";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Split a polygon with a cutter line.
 * @param call GRPC call with a target polygon and a cutter line.
 * @param callback Response callback. Contains output polygons resulting from the split, returned as Esri JSON strings.
 */
export const splitPolygonHandler: ArcGisServiceHandlers["splitPolygon"] = (call, callback) => {
  try {
    const target = esriJsonToPolygon(call.request.esriJsonPolygonTarget);
    const cutterLine = esriJsonToPolyline(call.request.esriJsonLineCutter);
    const polygons = splitPolygon(target, cutterLine);

    const response: string[] = (polygons || []).map(poly => JSON.stringify(poly.toJSON()));

    callback(null, { outputPolygonEsriJsons: response });
  } catch (error) {
    logger.error({ error }, "Error splitting polygon");
    callback(toGrpcInternalError(error), null);
  }
};
