import { describe, expect, test } from 'vitest';
import Polyline from '@arcgis/core/geometry/Polyline';
import { CoordinateSystem } from '../../../generated/arcgisjs/CoordinateSystem.ts';
import Point from '@arcgis/core/geometry/Point';
import {
  getIndexOfPointOnLine,
  getNearestParentStartAndEndNodes,
  isApproximatelyEqual,
  pointToEastWestLine,
  pointToNorthSouthLine,
} from '../../../src/migration/utils/migration-utils.ts';
import SpatialReference from '@arcgis/core/geometry/SpatialReference';
import { getCoordinateSystemWkid } from '../../../src/util/coordinate-system-utils.ts';

const ED50_WKID = getCoordinateSystemWkid(CoordinateSystem.ED50);

describe('migration-utils', () => {
  describe('getNearestParentStartAndEndNodes', () => {
    const parentPolyline = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 5],
          [0, 10],
        ],
      ],
      wkid: ED50_WKID,
    });

    test('Returns existing points that are on the polyline.', () => {
      const childStartPoint = new Point({
        x: 1,
        y: 0,
        spatialReference: ED50_WKID,
      });

      const childEndPoint = new Point({
        x: -1,
        y: 11,
        spatialReference: ED50_WKID,
      });

      const { nearestStartPoint, nearestEndPoint } = getNearestParentStartAndEndNodes(
        parentPolyline,
        childStartPoint,
        childEndPoint,
      );

      const expectedNearestStartPoint = new Point({
        x: 0,
        y: 0,
        spatialReference: ED50_WKID,
      });

      const expectedNearestEndPoint = new Point({
        x: 0,
        y: 10,
        spatialReference: ED50_WKID,
      });

      expect(nearestStartPoint.coordinate).toEqual(expectedNearestStartPoint);
      expect(nearestEndPoint.coordinate).toEqual(expectedNearestEndPoint);
    });

    test('Returns new points that are on the polyline.', () => {
      const childStartPoint = new Point({
        x: 1,
        y: 3,
        spatialReference: ED50_WKID,
      });

      const childEndPoint = new Point({
        x: -1,
        y: 7,
        spatialReference: ED50_WKID,
      });

      const { nearestStartPoint, nearestEndPoint } = getNearestParentStartAndEndNodes(
        parentPolyline,
        childStartPoint,
        childEndPoint,
      );

      const expectedNearestStartPoint = new Point({
        x: 0,
        y: 3,
        spatialReference: ED50_WKID,
      });

      const expectedNearestEndPoint = new Point({
        x: 0,
        y: 7,
        spatialReference: ED50_WKID,
      });

      expect(nearestStartPoint.coordinate).toEqual(expectedNearestStartPoint);
      expect(nearestEndPoint.coordinate).toEqual(expectedNearestEndPoint);
    });
  });

  describe('isApproximatelyEqual', () => {
    test('true', () => {
      const point1 = new Point({
        x: 0,
        y: 0,
        spatialReference: ED50_WKID,
      });
      const point2 = new Point({
        x: 0.00000000001,
        y: 0.00000000001,
        spatialReference: ED50_WKID,
      });

      expect(isApproximatelyEqual(point1, point2)).toEqual(true);
    });

    test('false', () => {
      const point1 = new Point({
        x: 0,
        y: 0,
        spatialReference: ED50_WKID,
      });
      const point2 = new Point({
        x: 0.0000000001,
        y: 0.0000000001,
        spatialReference: ED50_WKID,
      });

      expect(isApproximatelyEqual(point1, point2)).toEqual(false);
    });
  });

  test('pointToEastWestLine', () => {
    const expectedPolyline = new Polyline({
      paths: [
        [
          [-1, 0],
          [0, 0],
          [1, 0],
        ],
      ],
      spatialReference: ED50_WKID,
    });

    expect(pointToEastWestLine(0, 0, new SpatialReference({ wkid: ED50_WKID }), 1)).toEqual(expectedPolyline);
  });

  test('pointToNorthSouthLine', () => {
    const expectedPolyline = new Polyline({
      paths: [
        [
          [0, -1],
          [0, 0],
          [0, 1],
        ],
      ],
      spatialReference: ED50_WKID,
    });

    expect(pointToNorthSouthLine(0, 0, new SpatialReference({ wkid: ED50_WKID }), 1)).toEqual(expectedPolyline);
  });

  describe('getIndexOfPointOnLine', () => {
    const polyline = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 5],
          [0, 10],
          [0, 15],
        ],
      ],
      wkid: ED50_WKID,
    });

    test('points that match the polyline', () => {
      const point1 = new Point({
        x: 0,
        y: 0,
        spatialReference: { wkid: ED50_WKID },
      });
      const point2 = new Point({
        x: 0,
        y: 5,
        spatialReference: { wkid: ED50_WKID },
      });
      const point3 = new Point({
        x: 0,
        y: 10,
        spatialReference: { wkid: ED50_WKID },
      });
      const point4 = new Point({
        x: 0,
        y: 15,
        spatialReference: { wkid: ED50_WKID },
      });

      expect(getIndexOfPointOnLine(point1, polyline)).toEqual(0);
      expect(getIndexOfPointOnLine(point2, polyline)).toEqual(1);
      expect(getIndexOfPointOnLine(point3, polyline)).toEqual(2);
      expect(getIndexOfPointOnLine(point4, polyline)).toEqual(3);
    });

    test('points between points on the polyline', () => {
      const point1 = new Point({
        x: 0,
        y: 2,
        spatialReference: { wkid: ED50_WKID },
      });
      const point2 = new Point({
        x: 0,
        y: 3,
        spatialReference: { wkid: ED50_WKID },
      });

      expect(getIndexOfPointOnLine(point1, polyline)).toEqual(0);
      expect(getIndexOfPointOnLine(point2, polyline)).toEqual(1);
    });

    test('points near the polyline', () => {
      const point = new Point({
        x: 1,
        y: 5,
        spatialReference: { wkid: ED50_WKID },
      });
      expect(getIndexOfPointOnLine(point, polyline)).toEqual(1);
    });
  });
});
