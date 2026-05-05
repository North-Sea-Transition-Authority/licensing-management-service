import Polyline from '@arcgis/core/geometry/Polyline.js';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import * as cutOperator from '@arcgis/core/geometry/operators/cutOperator.js';
import * as multiPartToSinglePartOperator from '@arcgis/core/geometry/operators/multiPartToSinglePartOperator.js';
import * as containsOperator from '@arcgis/core/geometry/operators/containsOperator.js';
import * as unionOperator from '@arcgis/core/geometry/operators/unionOperator.js';
import type { ArcGisServiceHandlers } from '../../generated/arcgisjs/ArcGisService.js';
import { esriJsonToPolygon, esriJsonToPolyline } from '../util/esrijson-util';

/**
 * Split a polygon with a cutter line.
 * @param call GRPC call with a target polygon and a cutter line.
 * @param callback Response callback. Contains output polygons resulting from the split, returned as Esri JSON strings.
 */
export const splitPolygon: ArcGisServiceHandlers['splitPolygon'] = (call, callback) => {
  const target = esriJsonToPolygon(call.request.esriJsonPolygonTarget);
  const rawCutter = esriJsonToPolyline(call.request.esriJsonLineCutter);
  const cutter = removeOverlappingSegments(rawCutter);

  const cutResults = cutOperator.execute(target, cutter) as Polygon[];

  let polygons: Polygon[] = [];
  //Only separate polygons if a cut actually took place.
  if (cutResults.length > 0) {
    // cutResults may contain disjointed polygons, (one polygon that should actually be multiple polygons)
    // this operation splits those disjointed polygons into separate polygons
    polygons = multiPartToSinglePartOperator.executeMany(cutResults) as Polygon[];
  }

  const response: string[] = (polygons || []).map((poly) => JSON.stringify(poly.toJSON()));

  callback(null, { outputPolygonEsriJsons: response });
};

/**
 * Removes overlapping segments from a polyline.
 * If segment B is completely contained within segment A, segment B is removed.
 * This handles cases where a cutter line backtracks on itself.
 */
function removeOverlappingSegments(polyline: Polyline): Polyline {
  const segments = explodePolylineIntoSegments(polyline);

  if (segments.length <= 1) {
    return polyline;
  }

  const segmentIndexToRemove = new Set<number>();

  // Compare all pairs of segments
  for (let currentIndex = 0; currentIndex < segments.length; currentIndex++) {
    if (segmentIndexToRemove.has(currentIndex)) {
      continue;
    }

    for (let nextIndex = currentIndex + 1; nextIndex < segments.length; nextIndex++) {
      if (segmentIndexToRemove.has(nextIndex)) {
        continue;
      }

      if (containsOperator.execute(segments[currentIndex], segments[nextIndex])) {
        segmentIndexToRemove.add(nextIndex);
      } else if (containsOperator.execute(segments[nextIndex], segments[currentIndex])) {
        segmentIndexToRemove.add(currentIndex);
      }
    }
  }

  const remainingSegments = segments.filter((_, index) => !segmentIndexToRemove.has(index));
  if (remainingSegments.length === 0) {
    return polyline;
  }
  return unionOperator.executeMany(remainingSegments) as Polyline;
}

/**
 * Explode a complex polyline into its individual segments.
 * Each segment is represented as a separate polyline with two points (start and end).
 * @param polyline The polyline to explode.
 * @return An array of polylines, each representing a single segment of the original polyline.
 */
function explodePolylineIntoSegments(polyline: Polyline): Polyline[] {
  const segments: Polyline[] = [];

  polyline.paths.forEach((path) => {
    for (let i = 0; i < path.length - 1; i++) {
      const startPoint = path[i];
      const endPoint = path[i + 1];
      const segment = new Polyline({
        paths: [[startPoint, endPoint]],
        spatialReference: polyline.spatialReference,
      });
      segments.push(segment);
    }
  });

  return segments;
}
