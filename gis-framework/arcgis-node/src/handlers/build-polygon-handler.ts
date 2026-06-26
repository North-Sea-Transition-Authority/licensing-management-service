import type Polyline from "@arcgis/core/geometry/Polyline.js";
import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type { BuildPolygonResponse } from "../../generated/uk/co/fivium/grpc/gis/BuildPolygonResponse";
import { buildPolygon } from "../geometric-operators/build-polygon";
import { projectPolygonToWgs84 } from "../geometric-operators/project-polygon";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { asyncHandler } from "./async-handler";

/**
 * Builds a polygon from a list of polyline EsriJSONs.
 * @param call GRPC call with a list of polylines as EsriJSON strings and a spatial reference ID (WKID).
 * @param callback Response callback. Contains the resulting polygon as an EsriJSON string.
 */
export const buildPolygonHandler: ArcGisServiceHandlers["buildPolygon"] = asyncHandler(async (call): Promise<BuildPolygonResponse> => {
  const polylines: Polyline[] = call.request.esriJsonPolylines.map((lineJson: string) => esriJsonToPolyline(lineJson));
  let polygon = buildPolygon(polylines, call.request.coordinateSystemWkid);

  if (!polygon) {
    throw new Error("No polygons could be built from the provided polylines");
  }

  if (call.request.projectToWgs84) {
    polygon = await projectPolygonToWgs84(polygon);
  }

  return { polygonEsriJson: JSON.stringify(polygon.toJSON()) };
});
