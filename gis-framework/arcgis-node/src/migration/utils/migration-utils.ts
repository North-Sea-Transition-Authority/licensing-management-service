import Polyline from '@arcgis/core/geometry/Polyline';
import Point from '@arcgis/core/geometry/Point';
import * as proximityOperator from '@arcgis/core/geometry/operators/proximityOperator.js';
import SpatialReference from '@arcgis/core/geometry/SpatialReference';

/**
 * Finds the nearest points on a line for two given points. Intended to find the nearest start/end points on a parent line,
 * based on the start/end points of a child line.
 * @param parent The line where we want to find the nearest points.
 * @param childStartPoint The start point of the child line
 * @param childEndPoint The end point of the child line.
 * @returns The two nearest points on the parent line.
 */
export function getNearestParentStartAndEndNodes(parent: Polyline, childStartPoint: Point, childEndPoint: Point) {
  const nearestStartPoint = proximityOperator.getNearestCoordinate(parent, childStartPoint);
  const nearestEndPoint = proximityOperator.getNearestCoordinate(parent, childEndPoint);
  return { nearestStartPoint, nearestEndPoint };
}

/**
 * Checks to see if two points are equal within a 10 decimal place tolerance.
 * @param point1 First point
 * @param point2 Second point
 * @returns true if the points are equal within a 10 decimal place tolerance, false otherwise.
 */
export function isApproximatelyEqual(point1: Point, point2: Point): boolean {
  return Math.abs(point1.x - point2.x) < 1e-10 && Math.abs(point1.y - point2.y) < 1e-10;
}

/**
 * Converts a single coordinate into a Polyline by extending it by a given distance in both east and west directions.
 * @param longitude The longitude of the point.
 * @param latitude The latitude of the point.
 * @param srs the Spatial Reference of the point.
 * @param pointExtension The amount which the point should be extended in each direction, uses the same unit as the srs.
 * @returns A Polyline with a longitudinal length of pointExtension * 2, whose centre is the input longitude,
 *          and latitude.
 */
export function pointToEastWestLine(longitude: number, latitude: number, srs: SpatialReference, pointExtension: number) {
  return new Polyline({
    paths: [
      [
        [longitude - pointExtension, latitude],
        [longitude, latitude],
        [longitude + pointExtension, latitude],
      ],
    ],
    spatialReference: srs,
  });
}

/**
 * Converts a single coordinate into a Polyline by extending it by a given distance in both north and south directions.
 * @param longitude The longitude of the point.
 * @param latitude The latitude of the point.
 * @param srs the Spatial Reference of the point.
 * @param pointExtension The amount which the point should be extended in each direction, uses the same unit as the srs.
 * @returns A Polyline with a latitudinal length of pointExtension * 2, whose centre is the input longitude,
 *          and latitude.
 */
export function pointToNorthSouthLine(longitude: number, latitude: number, srs: SpatialReference, pointExtension: number) {
  return new Polyline({
    paths: [
      [
        [longitude, latitude - pointExtension],
        [longitude, latitude],
        [longitude, latitude + pointExtension],
      ],
    ],
    spatialReference: srs,
  });
}

/**
 * Returns a vertex index of a point on a line.
 * If the point matches a point on the polyline, it will return the index of that point on the line.
 * If the point is between two points, it will return the index of the nearest point on the line.
 * If the point is not on the line, it will return the index of the nearest point on the line.
 * @param point The point we want to find the index for.
 * @param polyline The line we want to find the index on.
 * @returns A number which is the vertex index of the point on the line.
 */
export function getIndexOfPointOnLine(point: Point, polyline: Polyline) {
  return proximityOperator.getNearestVertex(polyline, point).vertexIndex;
}
