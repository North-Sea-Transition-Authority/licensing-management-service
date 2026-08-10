import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import { coordinatesToPolyline } from "../geometric-operators/coordinates-to-polyline";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Builds an EsriJSON polyline from an ordered line of coordinates.
 * @param call the coordinates to build the polyline from, and the coordinate system they are expressed in.
 * @param callback Response callback. Contains the EsriJSON of the built polyline.
 */
export const coordinatesToPolylineHandler: ArcGisServiceHandlers["coordinatesToPolyline"] = (call, callback) => {
  try {
    const polylineEsriJson = coordinatesToPolyline(call.request.coordinates, call.request.srsWkid);
    callback(null, { polylineEsriJson });
  } catch (error) {
    callback(toGrpcInternalError(error), null);
  }
};
