import Polyline from '@arcgis/core/geometry/Polyline';
import { LineNavigationType } from '../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import { LineWithNavigationTypeAndId } from '../../src/migration/types/line-with-navigation-wrapper';
import Polygon from '@arcgis/core/geometry/Polygon.js';

export function makePolygonEsriJson(rings: number[][][]): string {
  return JSON.stringify({
    rings,
    spatialReference: { wkid: 4326 },
  });
}

export function makePolylineEsriJson(paths: number[][][]): string {
  return JSON.stringify({
    paths,
    spatialReference: { wkid: 4326 },
  });
}

export function makePolygon(rings: number[][][], wkid: number): Polygon {
  return Polygon.fromJSON({
    rings,
    spatialReference: { wkid },
  });
}

export function makePolyline(paths: number[][][], wkid: number): Polyline {
  return Polyline.fromJSON({
    paths,
    spatialReference: { wkid },
  });
}

export function makeLineWithNavigationAndId(
  line: Polyline,
  navigationType: LineNavigationType,
  id: number,
): LineWithNavigationTypeAndId {
  return { line, navigationType, id };
}
