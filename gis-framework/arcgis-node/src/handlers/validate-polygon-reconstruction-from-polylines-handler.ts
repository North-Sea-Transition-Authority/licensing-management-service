import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import { logger } from "../config/logger";
import {
  validatePolygonReconstructionFromPolylines,
} from "../geometric-operators/validate-polygon-reconstruction-from-polylines";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Validates that a polygon can be reconstructed from a list of ordered polylines.
 * @param call GRPC call with a list of polylines and the original polygon.
 * @param callback Response callback. Contains a boolean indicating whether the polygon can be reconstructed.
 */
export const validatePolygonReconstructionFromPolylinesHandler: ArcGisServiceHandlers["validatePolygonReconstructionFromPolylines"]
  = (call, callback) => {
    try {
      const polylines = call.request.esriJsonPolylines.map(esriJsonPolyline => (esriJsonToPolyline(esriJsonPolyline)));
      const isValid = validatePolygonReconstructionFromPolylines(polylines, call.request.originalPolygonEsriJson);
      callback(null, { isValid });
    } catch (error) {
      logger.error({ error }, "Error validating polygon reconstruction from polylines");
      callback(toGrpcInternalError(error), null);
    }
  };
