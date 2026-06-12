import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type { ArcGisServiceHandlers } from "../../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type {
  EsriJsonPolygonLineWrappers__Output,
} from "../../../generated/uk/co/fivium/grpc/gis/EsriJsonPolygonLineWrappers";
import type { EsriJsonLineStringToIsGeodesic } from "../verify-child-geodesic-lines-overlap-parents";
import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import { logger } from "../../config/logger";
import { linesToSinglePolygon } from "../../geometric-operators/lines-to-single-polygon-operator";
import { unionPolygonsOperator } from "../../geometric-operators/union-polygons-operator";
import { toGrpcInternalError } from "../../handlers/grpc-error";
import { getCoordinateSystemWkid } from "../../util/coordinate-system-utils";
import { childGeodesicLinesOverlapParents } from "../verify-child-geodesic-lines-overlap-parents";

/**
 * This validates that the child polygon is contained by the parent polygon and that the geodesic lines of the child overlap with
 * the geodesic lines of the parent, if any.
 *
 * @param call {@link BlockAndSubareaValidationRequest}
 * @param callback {@link ValidationResponse}
 */
export const validateBlockAndSubarea: ArcGisServiceHandlers["validateBlockAndSubarea"] = (call, callback) => {
  try {
    const { childPolygonLineWrappersLists, parentPolygonLineWrappersLists, coordinateSystem } = call.request;
    const wkid = getCoordinateSystemWkid(coordinateSystem);

    const { unionedPolygon: unionedChildPolygons, lines: childLines } = processPolygons(childPolygonLineWrappersLists, wkid);
    const { unionedPolygon: unionedParentPolygons, lines: parentLines } = processPolygons(parentPolygonLineWrappersLists, wkid);

    const isChildContainedByParent = containsOperator.execute(unionedParentPolygons, unionedChildPolygons);

    if (!isChildContainedByParent) {
      callback(null, { isValid: false, message: "Child is not contained by parent" });
      return;
    }

    if (!childGeodesicLinesOverlapParents(parentLines, childLines)) {
      callback(null, { isValid: false, message: "Child geodesic lines do not overlap with parent geodesic lines" });
      return;
    }

    callback(null, { isValid: true });
  } catch (error) {
    logger.error({ error }, "Error validating block and subarea");
    callback(toGrpcInternalError(error), null);
  }
};

function processPolygons(
  polygonInputs: EsriJsonPolygonLineWrappers__Output[],
  wkid: number,
): { unionedPolygon: Polygon, lines: EsriJsonLineStringToIsGeodesic[] } {
  const polygons: Polygon[] = [];
  const lines: EsriJsonLineStringToIsGeodesic[] = [];
  polygonInputs.forEach((polygon) => {
    const polylines: Polyline[] = [];
    polygon.lineWrappers.forEach((line) => {
      const polyline = Polyline.fromJSON(JSON.parse(line.esriJsonString));
      polylines.push(polyline);
      lines.push({
        esriJsonPolyline: line.esriJsonString,
        isGeodesic: line.navigationType === LineNavigationType.GEODESIC,
      });
    });
    polygons.push(linesToSinglePolygon(polylines, wkid));
  });
  return { unionedPolygon: unionPolygonsOperator(polygons), lines };
}
