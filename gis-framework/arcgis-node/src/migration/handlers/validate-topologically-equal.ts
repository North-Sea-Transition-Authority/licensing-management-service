import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type { ArcGisServiceHandlers } from "../../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type { EsriJsonPolygonLines__Output } from "../../../generated/uk/co/fivium/grpc/gis/EsriJsonPolygonLines";
import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { linesToSinglePolygon } from "../../geometric-operators/lines-to-single-polygon-operator";
import { unionPolygonsOperator } from "../../geometric-operators/union-polygons-operator";
import { getCoordinateSystemWkid } from "../../util/coordinate-system-utils";

/**
 * This validates that the parent polygons are topologically equal to the child polygons.
 *
 * @param call {@link TopologicallyEqualValidationRequest}
 * @param callback {@link ValidationResponse}
 */
export const validateTopologicallyEqual: ArcGisServiceHandlers["validateTopologicallyEqual"] = async (call, callback) => {
  const { childPolygons, parentPolygons, coordinateSystem } = call.request;
  const unionedChildPolygon = processPolygons(childPolygons, getCoordinateSystemWkid(coordinateSystem));
  const unionedParentPolygon = processPolygons(parentPolygons, getCoordinateSystemWkid(coordinateSystem));

  const isTopologicallyEqual = equalsOperator.execute(unionedChildPolygon, unionedParentPolygon);

  if (!isTopologicallyEqual) {
    callback(null, { isValid: false, message: "Polygons are not topologically equal" });
    return;
  }
  callback(null, { isValid: true });
};

function processPolygons(polygonsAsLines: EsriJsonPolygonLines__Output[], wkid: number): Polygon {
  const polygons: Polygon[] = [];
  polygonsAsLines.forEach((polygon) => {
    const polylines: Polyline[] = [];
    polygon.esriJsonPolyline.forEach((line) => {
      const polyline = Polyline.fromJSON(JSON.parse(line));
      polylines.push(polyline);
    });
    polygons.push(linesToSinglePolygon(polylines, wkid));
  });
  return unionPolygonsOperator(polygons);
}
