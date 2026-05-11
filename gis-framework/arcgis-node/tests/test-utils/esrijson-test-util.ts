import Polyline from '@arcgis/core/geometry/Polyline';
import Point from '@arcgis/core/geometry/Point';
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

export function makePoint(x: number, y: number, wkid: number): Point {
  return new Point({
    x,
    y,
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

export function makeRectanglePolygonWrapper(
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  navigationType = LineNavigationType.LOXODROME,
) {
  return {
    lineWrappers: [
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x1, y1],
            [x2, y1],
          ],
        ]),
        oracleLineSsid: 1,
        navigationType,
      },
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x2, y1],
            [x2, y2],
          ],
        ]),
        oracleLineSsid: 2,
        navigationType,
      },
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x2, y2],
            [x1, y2],
          ],
        ]),
        oracleLineSsid: 3,
        navigationType,
      },
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x1, y2],
            [x1, y1],
          ],
        ]),
        oracleLineSsid: 4,
        navigationType,
      },
    ],
  };
}
