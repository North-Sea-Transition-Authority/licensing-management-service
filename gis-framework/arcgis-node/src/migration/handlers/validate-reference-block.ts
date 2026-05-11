import { ArcGisServiceHandlers } from '../../../generated/uk/co/fivium/grpc/gis/ArcGisService';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { getCoordinateSystemWkid } from '../../util/coordinate-system-utils';
import { linesToSinglePolygon } from '../../geometric-operators/lines-to-single-polygon-operator';
import * as containsOperator from '@arcgis/core/geometry/operators/containsOperator.js';
import { unionPolygonsOperator } from '../../geometric-operators/union-polygons-operator';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import { childGeodesicLinesOverlapParents, EsriJsonLineStringToIsGeodesic } from '../verify-child-geodesic-lines-overlap-parents';
import { EsriJsonPolygonLineWrappers__Output } from '../../../generated/uk/co/fivium/grpc/gis/EsriJsonPolygonLineWrappers';
import { esriJsonToPolyline } from '../../util/esrijson-util';
import { logger } from '../../config/logger';
import { toGrpcInternalError } from '../../handlers/grpc-error';

/**
 * This validates that all license block polygons are contained within the reference block polygon
 * and that the geodesic lines of each license block overlap with the geodesic lines of the reference block.
 *
 * @param call {@link ReferenceBlockValidationRequest}
 * @param callback {@link ValidationResponse}
 */
export const validateReferenceBlock: ArcGisServiceHandlers['validateReferenceBlock'] = async (call, callback) => {
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
    logger.error({ error: error }, 'Error validating reference block');
    callback(toGrpcInternalError(error), null);
  }
};

function processPolygons(
  polygonInputs: EsriJsonPolygonLineWrappers__Output[],
  wkid: number,
): { unionedPolygon: Polygon; lines: EsriJsonLineStringToIsGeodesic[] } {
  const polygons: Polygon[] = [];
  const lines: EsriJsonLineStringToIsGeodesic[] = [];
  polygonInputs.forEach((polygon) => {
    const polylines: Polyline[] = [];
    polygon.lineWrappers.forEach((line) => {
      const polyline = esriJsonToPolyline(line.esriJsonString);
      polylines.push(polyline);
      lines.push({
        esriJsonPolyline: line.esriJsonString,
        isGeodesic: line.navigationType != LineNavigationType.LOXODROME,
      });
    });
    polygons.push(linesToSinglePolygon(polylines, wkid));
  });
  return { unionedPolygon: unionPolygonsOperator(polygons), lines };
}
