import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type { LineWithId } from "../geometric-operators/find-northwest-most-line";
import { logger } from "../config/logger";
import { findNorthwestMostLine } from "../geometric-operators/find-northwest-most-line";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Finds the line with the northwest-most starting point in a list of lines.
 * @param call GRPC call with a list of lines.
 * @param callback Response callback. Contains the ID of the line with the northwest-most starting point.
 */
export const findNorthwestMostLineHandler: ArcGisServiceHandlers["findNorthwestMostLine"] = (call, callback) => {
  try {
    const linesWithId: LineWithId[] = call.request.lines.map((line) => {
      return {
        id: line.id,
        polyline: esriJsonToPolyline(line.polyLineEsriJson),
      };
    });
    const lineId = findNorthwestMostLine(linesWithId);
    callback(null, { lineId });
  } catch (error) {
    logger.error({ error }, "Error finding northwest-most line");
    callback(toGrpcInternalError(error), null);
  }
};
