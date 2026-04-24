import { describe, expect, test } from 'vitest';
import Polyline from '@arcgis/core/geometry/Polyline';
import { CoordinateSystem } from '../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts';
import Point from '@arcgis/core/geometry/Point';
import {
  getIndexOfPointOnLine,
  getNearestParentStartAndEndNodes,
  GEODESIC_DENSE_POINT_METERS_INTERVAL,
  GENERALIZE_TOLERANCE_DEGREES,
  isApproximatelyEqual,
  migrateBlock,
  pointToEastWestLine,
  pointToNorthSouthLine,
  mergeParentDensePointsIntoChildLine,
  shiftNodeAndUpdateConnectedLine,
  findPointOfIntersectionBetweenChildPointOnBearingAndParentLine,
  findLineConnectingToPointNotOnBearing,
  ONE_ARC_SECOND,
} from '../../../src/migration/utils/migration-utils.ts';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts';
import SpatialReference from '@arcgis/core/geometry/SpatialReference';
import { getCoordinateSystemWkid } from '../../../src/util/coordinate-system-utils.ts';
import * as geodeticDensifyOperator from '@arcgis/core/geometry/operators/geodeticDensifyOperator.js';
import * as generalizeOperator from '@arcgis/core/geometry/operators/generalizeOperator.js';
import { SetBearing } from '../../../src/migration/types/line-with-bearing-wrapper.ts';

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

  describe('migrateBlock', () => {
    test('densifies geodesic lines but leaves loxodrome lines unchanged', async () => {
      const loxodromeLine = new Polyline({
        paths: [
          [
            [0, 50],
            [1, 51],
          ],
        ],
        spatialReference: { wkid: ED50_WKID },
      });

      const geodesicLine = new Polyline({
        paths: [
          [
            [0, 50],
            [1, 51],
          ],
        ],
        spatialReference: { wkid: ED50_WKID },
      });

      const input = [
        { line: loxodromeLine, navigationType: LineNavigationType.LOXODROME, id: 1 },
        { line: geodesicLine, navigationType: LineNavigationType.GEODESIC, id: 2 },
      ];

      const result = await migrateBlock(input);

      if (!geodeticDensifyOperator.isLoaded()) {
        await geodeticDensifyOperator.load();
      }
      const densifiedGeodesicLine = generalizeOperator.execute(
        geodeticDensifyOperator.execute(geodesicLine, GEODESIC_DENSE_POINT_METERS_INTERVAL, {
          curveType: 'geodesic',
          unit: 'meters',
        }),
        GENERALIZE_TOLERANCE_DEGREES,
      );

      const expected = [
        { line: loxodromeLine, navigationType: LineNavigationType.LOXODROME, id: 1 },
        { line: densifiedGeodesicLine, navigationType: LineNavigationType.GEODESIC, id: 2 },
      ];

      expect(result).toEqual(expected);
    });
  });

  test('mergeParentDensePointsIntoChildLine', () => {
    const srs = new SpatialReference({ wkid: ED50_WKID });
    const parentLine = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 1],
          [0, 2],
          [0, 3],
          [0, 4],
          [0, 5],
        ],
      ],
      spatialReference: srs,
    });

    const childStartPoint = new Point({
      x: 0,
      y: 1,
      spatialReference: srs,
    });

    const childEndPoint = new Point({
      x: 0,
      y: 4,
      spatialReference: srs,
    });

    const expectedPolyline = new Polyline({
      paths: [
        [
          [0, 1],
          [0, 2],
          [0, 3],
          [0, 4],
        ],
      ],
      spatialReference: srs,
    });

    const result = mergeParentDensePointsIntoChildLine(parentLine, childStartPoint, childEndPoint, srs);
    expect(result).toEqual(expectedPolyline);
  });

  describe('shiftNodeAndUpdateConnectedLine', () => {
    const srs = new SpatialReference({ wkid: ED50_WKID });

    const childPoint = new Point({
      x: 1.0,
      y: 50.0,
      spatialReference: srs,
    });

    const nearestCoordinate = new Point({
      x: 1.0001,
      y: 50.0001,
      spatialReference: srs,
    });

    const childId = 100;
    const nodeType = 'start';

    const parentGeodesicLine = new Polyline({
      paths: [
        [
          [0.5, 50.0],
          [1.5, 50.0],
        ],
      ],
      spatialReference: srs,
    });

    test('line on set bearing and intersection is found', () => {
      const loxodromeOnBearing = new Polyline({
        paths: [
          [
            [1.0, 50.0],
            [1.0, 50.001],
          ],
        ],
        spatialReference: srs,
      });

      const idToLineWrapper = new Map();
      idToLineWrapper.set(200, {
        line: loxodromeOnBearing,
        navigationType: LineNavigationType.LOXODROME,
        id: 200,
      });

      const result = shiftNodeAndUpdateConnectedLine(
        childPoint,
        nearestCoordinate,
        childId,
        idToLineWrapper,
        parentGeodesicLine,
        nodeType,
      );

      const expectedIntersection = new Point({
        x: 1.0,
        y: 50.0,
        spatialReference: srs,
      });

      expect(result).toEqual(expectedIntersection);

      const expectedUpdatedLine = new Polyline({
        paths: [
          [
            [expectedIntersection.x, expectedIntersection.y],
            [1.0, 50.001],
          ],
        ],
        spatialReference: srs,
      });

      const expectedIdToLineWrapper = new Map();
      expectedIdToLineWrapper.set(200, {
        line: expectedUpdatedLine,
        navigationType: LineNavigationType.LOXODROME,
        id: 200,
      });

      expect(idToLineWrapper).toEqual(expectedIdToLineWrapper);
    });

    test('line on set bearing and no intersection is found', () => {
      const loxodromeOnBearing = new Polyline({
        paths: [
          [
            [1.0, 50.0],
            [1.0, 50.001],
          ],
        ],
        spatialReference: srs,
      });

      const idToLineWrapper = new Map();
      idToLineWrapper.set(200, {
        line: loxodromeOnBearing,
        navigationType: LineNavigationType.LOXODROME,
        id: 200,
      });

      const farAwayParentLine = new Polyline({
        paths: [
          [
            [5.0, 55.0],
            [6.0, 56.0],
          ],
        ],
        spatialReference: srs,
      });

      expect(() =>
        shiftNodeAndUpdateConnectedLine(childPoint, nearestCoordinate, childId, idToLineWrapper, farAwayParentLine, nodeType),
      ).toThrow('No intersection point for line 200 on set bearing was found.');
    });

    test('line not on set bearing and a connection is found', () => {
      const geodesicLine = new Polyline({
        paths: [
          [
            [1.0, 50.0],
            [2.0, 51.0],
          ],
        ],
        spatialReference: srs,
      });

      const idToLineWrapper = new Map();
      idToLineWrapper.set(200, {
        line: geodesicLine,
        navigationType: LineNavigationType.GEODESIC,
        id: 200,
      });

      const result = shiftNodeAndUpdateConnectedLine(
        childPoint,
        nearestCoordinate,
        childId,
        idToLineWrapper,
        parentGeodesicLine,
        nodeType,
      );

      expect(result).toEqual(nearestCoordinate);

      const expectedUpdatedLine = new Polyline({
        paths: [
          [
            [nearestCoordinate.x, nearestCoordinate.y],
            [2.0, 51.0],
          ],
        ],
        spatialReference: srs,
      });

      const expectedIdToLineWrapper = new Map();
      expectedIdToLineWrapper.set(200, {
        line: expectedUpdatedLine,
        navigationType: LineNavigationType.GEODESIC,
        id: 200,
      });

      expect(idToLineWrapper).toEqual(expectedIdToLineWrapper);
    });

    test('line not on set bearing and no connection is found', () => {
      const childLine = new Polyline({
        paths: [
          [
            [1.0, 50.0],
            [2.0, 51.0],
          ],
        ],
        spatialReference: srs,
      });

      const idToLineWrapper = new Map();
      idToLineWrapper.set(childId, {
        line: childLine,
        navigationType: LineNavigationType.GEODESIC,
        id: childId,
      });

      expect(() =>
        shiftNodeAndUpdateConnectedLine(childPoint, nearestCoordinate, childId, idToLineWrapper, parentGeodesicLine, nodeType),
      ).toThrow(`No line connecting to ${nodeType} node with id ${childId} was found.`);
    });
  });

  describe('findPointOfIntersectionBetweenChildPointOnBearingAndParentLine', () => {
    const srs = new SpatialReference({ wkid: ED50_WKID });

    test('latitude bearing with intersection', () => {
      // Point at (1, 50), latitude bearing creates a N-S line
      const childPoint = new Point({ x: 1, y: 50, spatialReference: srs });

      // Horizontal parent line crossing through x=1 at y=50
      const parent = new Polyline({
        paths: [
          [
            [0, 50],
            [2, 50],
          ],
        ],
        spatialReference: srs,
      });

      const result = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
        childPoint,
        SetBearing.LATITUDE,
        parent,
        ONE_ARC_SECOND,
      );

      const expectedIntersection = new Point({ x: 1, y: 50, spatialReference: srs });
      expect(result).toEqual(expectedIntersection);
    });

    test('latitude bearing with no intersection', () => {
      const childPoint = new Point({ x: 1, y: 50, spatialReference: srs });

      // Parent line far away from x=1
      const parent = new Polyline({
        paths: [
          [
            [5, 55],
            [6, 56],
          ],
        ],
        spatialReference: srs,
      });

      const result = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
        childPoint,
        SetBearing.LATITUDE,
        parent,
        ONE_ARC_SECOND,
      );

      expect(result).toBeUndefined();
    });

    test('longitude bearing with intersection', () => {
      // Point at (1, 50), longitude bearing creates an E-W line
      const childPoint = new Point({ x: 1, y: 50, spatialReference: srs });

      // Vertical parent line crossing through y=50 at x=1
      const parent = new Polyline({
        paths: [
          [
            [1, 49],
            [1, 51],
          ],
        ],
        spatialReference: srs,
      });

      const result = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
        childPoint,
        SetBearing.LONGITUDE,
        parent,
        ONE_ARC_SECOND,
      );

      const expectedIntersection = new Point({ x: 1, y: 50, spatialReference: srs });
      expect(result).toEqual(expectedIntersection);
    });

    test('longitude bearing with no intersection', () => {
      const childPoint = new Point({ x: 1, y: 50, spatialReference: srs });

      // Parent line far away from y=50
      const parent = new Polyline({
        paths: [
          [
            [5, 55],
            [6, 56],
          ],
        ],
        spatialReference: srs,
      });

      const result = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
        childPoint,
        SetBearing.LONGITUDE,
        parent,
        ONE_ARC_SECOND,
      );

      expect(result).toBeUndefined();
    });
  });

  describe('findLineConnectingToPointNotOnBearing', () => {
    const srs = new SpatialReference({ wkid: ED50_WKID });
    const point = new Point({ x: 1, y: 50, spatialReference: srs });
    const targetLineId = 100;

    test('no connection found', () => {
      const unconnectedLine = new Polyline({
        paths: [
          [
            [5, 55],
            [6, 56],
          ],
        ],
        spatialReference: srs,
      });

      const lines = [{ line: unconnectedLine, navigationType: LineNavigationType.LOXODROME, id: 200 }];

      const result = findLineConnectingToPointNotOnBearing(point, targetLineId, lines);
      expect(result).toBeUndefined();
    });

    test('line connecting at its start point', () => {
      const lineConnectingAtStart = new Polyline({
        paths: [
          [
            [1, 50],
            [2, 51],
          ],
        ],
        spatialReference: srs,
      });

      const expectedLine = { line: lineConnectingAtStart, navigationType: LineNavigationType.LOXODROME, id: 200 };

      const lines = [expectedLine];

      const result = findLineConnectingToPointNotOnBearing(point, targetLineId, lines);
      expect(result).toEqual(expectedLine);
    });

    test('line connecting at its end point', () => {
      const lineConnectingAtEnd = new Polyline({
        paths: [
          [
            [2, 51],
            [1, 50],
          ],
        ],
        spatialReference: srs,
      });

      const expectedLine = { line: lineConnectingAtEnd, navigationType: LineNavigationType.LOXODROME, id: 300 };

      const lines = [expectedLine];

      const result = findLineConnectingToPointNotOnBearing(point, targetLineId, lines);
      expect(result).toEqual(expectedLine);
    });
  });
});
