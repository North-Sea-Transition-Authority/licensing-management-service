import type Polyline from "@arcgis/core/geometry/Polyline.js";
import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import { buildPolygon } from "../geometric-operators/build-polygon";
import { projectToWgs84 } from "../geometric-operators/project-polygon";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Builds a polygon from a list of polyline EsriJSONs.
 * @param call GRPC call with a list of polylines as EsriJSON strings and a spatial reference ID (WKID).
 * @param callback Response callback. Contains the resulting polygon as an EsriJSON string.
 */
export const buildPolygonHandler: ArcGisServiceHandlers["buildPolygon"] = async (call, callback) => {
  try {
    const polylines: Polyline[] = call.request.esriJsonPolylines.map((lineJson: string) => esriJsonToPolyline(lineJson));
    let polygon = buildPolygon(polylines, call.request.coordinateSystemWkid);

    if (!polygon) {
      callback(new Error("No polygons could be built from the provided polylines"), null);
      return;
    }

    if (call.request.projectToWgs84) {
      polygon = await projectToWgs84(polygon);
    }

    callback(null, { polygonEsriJson: JSON.stringify(polygon.toJSON()) });
  } catch (error) {
    console.error({ error }, "Error building polygon");
    callback(toGrpcInternalError(error), null);
  }
};
