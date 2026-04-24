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

/**
 * This validates that the child polygon is contained by the parent polygon and that the geodesic lines of the child overlap with
 * the geodesic lines of the parent, if any.
 *
 * @param call {@link BlockAndSubareaValidationRequest}
 * @param callback {@link ValidationResponse}
 */
export const validateBlockAndSubarea: ArcGisServiceHandlers['validateBlockAndSubarea'] = async (call, callback) => {
  const { childPolygonLineWrappersLists, parentPolygonLineWrappersLists, coordinateSystem } = call.request;
  const wkid = getCoordinateSystemWkid(coordinateSystem);

  const { unionedPolygon: unionedChildPolygons, lines: childLines } = processPolygons(childPolygonLineWrappersLists, wkid);
  const { unionedPolygon: unionedParentPolygons, lines: parentLines } = processPolygons(parentPolygonLineWrappersLists, wkid);

  const isChildContainedByParent = containsOperator.execute(unionedParentPolygons, unionedChildPolygons);

  if (!isChildContainedByParent) {
    callback(null, { isValid: false, message: 'Child is not contained by parent' });
    return;
  }

  if (!childGeodesicLinesOverlapParents(parentLines, childLines)) {
    callback(null, { isValid: false, message: 'Child geodesic lines do not overlap with parent geodesic lines' });
    return;
  }

  callback(null, { isValid: true });
};

function processPolygons(
  polygonInputs: EsriJsonPolygonLineWrappers__Output[],
  wkid: number,
): { unionedPolygon: Polygon; lines: EsriJsonLineStringToIsGeodesic[] } {
  const polygons: Polygon[] = [];
  const lines: EsriJsonLineStringToIsGeodesic[] = [];
  polygonInputs.forEach((polygon) => {
    const polylines: Polyline[] = [];
    polygon.lineWrapper.forEach((line) => {
      const polyline = Polyline.fromJSON(JSON.parse(line.esriJsonString));
      polylines.push(polyline);
      lines.push({
        esriJsonPolyline: line.esriJsonString,
        isGeodesic: line.navigationType == LineNavigationType.GEODESIC,
      });
    });
    polygons.push(linesToSinglePolygon(polylines, wkid));
  });
  return { unionedPolygon: unionPolygonsOperator(polygons), lines };
}
