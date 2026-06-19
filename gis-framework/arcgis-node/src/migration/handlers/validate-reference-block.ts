import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type Polyline from "@arcgis/core/geometry/Polyline.js";
import type { ArcGisServiceHandlers } from "../../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type {
  EsriJsonPolygonLineWrappers__Output,
} from "../../../generated/uk/co/fivium/grpc/gis/EsriJsonPolygonLineWrappers";
import type { EsriJsonLineStringToIsGeodesic } from "../verify-child-geodesic-lines-overlap-parents";
import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import { CoordinateSystem } from "../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import { logger } from "../../config/logger";
import { linesToSinglePolygon } from "../../geometric-operators/lines-to-single-polygon-operator";
import { unionPolygonsOperator } from "../../geometric-operators/union-polygons-operator";
import { toGrpcInternalError } from "../../handlers/grpc-error";
import { getCoordinateSystemWkid } from "../../util/coordinate-system-utils";
import { esriJsonToPolyline } from "../../util/esrijson-util";
import { childGeodesicLinesOverlapParents } from "../verify-child-geodesic-lines-overlap-parents";

/**
 * This validates that all license block polygons are contained within the reference block polygon
 * and that the geodesic lines of each license block overlap with the geodesic lines of the reference block.
 *
 * @param call {@link ReferenceBlockValidationRequest}
 * @param callback {@link ValidationResponse}
 */
export const validateReferenceBlock: ArcGisServiceHandlers["validateReferenceBlock"] = (call, callback) => {
  try {
    const { refBlockPolygonLineWrappersList, licenceBlockPolygonLineWrappersList, coordinateSystem } = call.request;
    const wkid = getCoordinateSystemWkid(coordinateSystem);

    const { unionedPolygon: unionedRefBlockPolygon, lines: refBlockLines } = processPolygons(
      refBlockPolygonLineWrappersList,
      wkid,
    );
    for (const licenceBlockPolygonLineWrappers of licenceBlockPolygonLineWrappersList) {
      const { unionedPolygon: unionedLicenseBlockPolygon, lines: licenseBlockLines } = processPolygons(
        [licenceBlockPolygonLineWrappers],
        wkid,
      );
      if (!containsOperator.execute(unionedRefBlockPolygon, unionedLicenseBlockPolygon)) {
        logger.info(`unionedRefBlockPolygon: ${JSON.stringify(unionedRefBlockPolygon.toJSON())}`);
        logger.info(`unionedLicenseBlockPolygon: ${JSON.stringify(unionedLicenseBlockPolygon.toJSON())}`);

        callback(null, {
          isValid: false,
          message: `Reference block does not contain all of its licence blocks.`,
        });
        return;
      }

      if (!childGeodesicLinesOverlapParents(refBlockLines, licenseBlockLines)) {
        callback(null, {
          isValid: false,
          message: `License block geodesic lines do not overlap reference block geodesic lines.`,
        });
        return;
      }
    }

    callback(null, { isValid: true });
  } catch (error) {
    logger.error({ error }, "Error validating reference block");
    callback(toGrpcInternalError(error), null);
  }
};

export function processPolygons(
  polygonInputs: EsriJsonPolygonLineWrappers__Output[],
  wkid: number,
): { unionedPolygon: Polygon, lines: EsriJsonLineStringToIsGeodesic[] } {
  const polygons: Polygon[] = [];
  const lines: EsriJsonLineStringToIsGeodesic[] = [];
  const bngWkid = getCoordinateSystemWkid(CoordinateSystem.BRITISH_NATIONAL_GRID);

  polygonInputs.forEach((polygon) => {
    const polylines: Polyline[] = [];
    polygon.lineWrappers.forEach((line) => {
      const polyline = esriJsonToPolyline(line.esriJsonString);

      // If offshore then treat cartesian lines as geodesic, else treat them as non geodesic.
      const isGeodesic = (bngWkid !== wkid && line.navigationType !== LineNavigationType.LOXODROME)
        || (line.navigationType === LineNavigationType.GEODESIC);

      polylines.push(polyline);
      lines.push({
        esriJsonPolyline: line.esriJsonString,
        isGeodesic,
      });
    });
    polygons.push(linesToSinglePolygon(polylines, wkid));
  });
  return { unionedPolygon: unionPolygonsOperator(polygons), lines };
}
