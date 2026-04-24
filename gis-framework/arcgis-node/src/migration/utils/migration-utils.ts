import Polyline from '@arcgis/core/geometry/Polyline';
import Point from '@arcgis/core/geometry/Point';
import * as proximityOperator from '@arcgis/core/geometry/operators/proximityOperator.js';
import * as intersectionOperator from '@arcgis/core/geometry/operators/intersectionOperator.js';
import * as intersectsOperator from '@arcgis/core/geometry/operators/intersectsOperator.js';
import SpatialReference from '@arcgis/core/geometry/SpatialReference';
import { LineWithNavigationTypeAndId } from '../types/line-with-navigation-wrapper';
import { logger } from '../../config/logger';
import * as geodeticDensifyOperator from '@arcgis/core/geometry/operators/geodeticDensifyOperator.js';
import * as generalizeOperator from '@arcgis/core/geometry/operators/generalizeOperator.js';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import { findLoxodromeThatConnectsToPointOnSetBearing, SetBearing } from '../types/line-with-bearing-wrapper';
import Multipoint from '@arcgis/core/geometry/Multipoint';

// 1 second in degrees (arc second) == 1° (degree) / 60'(minutes) / 60" (seconds)
export const ONE_ARC_SECOND = 1 / 3600;
export const GEODESIC_DENSE_POINT_METERS_INTERVAL = 100;
// Tolerance for generalizing line geometries in degrees (~1 mm on ED50).
export const GENERALIZE_TOLERANCE_DEGREES = 0.00000001;

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
export function pointToEastWestLine(
  longitude: number,
  latitude: number,
  srs: SpatialReference,
  pointExtension: number,
): Polyline {
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
export function pointToNorthSouthLine(
  longitude: number,
  latitude: number,
  srs: SpatialReference,
  pointExtension: number,
): Polyline {
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
export function getIndexOfPointOnLine(point: Point, polyline: Polyline): number {
  return proximityOperator.getNearestVertex(polyline, point).vertexIndex;
}

/**
 * This function migrates a parentless shape / block by densifying any geodesic lines at a rate of {@link GEODESIC_DENSE_POINT_METERS_INTERVAL},
 * and then generalizes the line to get rid of any unnecessary points using the Douglas-Peucker algorithm.
 * Non geodesic lines are left unchanged.
 *
 * https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/geodeticDensifyOperator/
 * https://developers.arcgis.com/javascript/latest/references/core/geometry/operators/generalizeOperator/
 * https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
 *
 * @param linesWithNavigationTypeAndId A list of unprocessed Polylines with their navigation type and id.
 * @returns A processed list of {@link LineWithNavigationTypeAndId}.
 */
export async function migrateBlock(
  linesWithNavigationTypeAndId: LineWithNavigationTypeAndId[],
): Promise<LineWithNavigationTypeAndId[]> {
  logger.info('Migrating block');
  if (!geodeticDensifyOperator.isLoaded()) {
    await geodeticDensifyOperator.load();
  }

  const convertedLines: LineWithNavigationTypeAndId[] = [];
  for (const lineObject of linesWithNavigationTypeAndId) {
    let { line } = lineObject;
    const { navigationType, id } = lineObject;

    if (navigationType === LineNavigationType.GEODESIC) {
      line = geodeticDensifyOperator.execute(line, GEODESIC_DENSE_POINT_METERS_INTERVAL, {
        curveType: 'geodesic',
        unit: 'meters',
      }) as Polyline;
      line = generalizeOperator.execute(line, GENERALIZE_TOLERANCE_DEGREES) as Polyline;
    }

    convertedLines.push({ line: line, id: id, navigationType: navigationType });
  }
  return convertedLines;
}

/**
 * This function copies down all the points from a line that are between two points. To be used so that geodesic lines in a
 * subarea (child) can perfectly overlap geodesic licence block (parent) lines.
 *
 * @param parent The {@link Polyline} whose points we want to copy.
 * @param childStartPoint The start point of the new line.
 * @param childEndPoint The end point of the new line.
 * @param srs The spatial reference to use for the new line.
 * @returns a {@link Polyline} whose start and end points are the same as the inputted points, and the points between are copied
 * from the parent.
 */
export function mergeParentDensePointsIntoChildLine(
  parent: Polyline,
  childStartPoint: Point,
  childEndPoint: Point,
  srs: SpatialReference,
): Polyline {
  const { nearestStartPoint, nearestEndPoint } = getNearestParentStartAndEndNodes(parent, childStartPoint, childEndPoint);

  const newPath = [[childStartPoint.x, childStartPoint.y]];

  const startIndex = nearestStartPoint.vertexIndex;
  const endIndex = nearestEndPoint.vertexIndex;

  if (startIndex <= endIndex) {
    for (let i = startIndex + 1; i <= endIndex; i++) {
      newPath.push([parent.paths[0][i][0], parent.paths[0][i][1]]);
    }
  } else {
    for (let i = startIndex - 1; i >= endIndex; i--) {
      newPath.push([parent.paths[0][i][0], parent.paths[0][i][1]]);
    }
  }

  newPath.push([childEndPoint.x, childEndPoint.y]);

  return new Polyline({
    paths: [newPath],
    spatialReference: srs,
  });
}

/**
 * This method is for updating the start/end that connect to a child geodesic line so that it overlaps the parent geodesic line,
 * by first finding the line that connects to the child point, then checking if that line is on a set bearing (latitude or longitude).
 * If it is on a set bearing, the line is extended so that we can find the intersection between the line and the parent geodesic line, and then shift the point to be the intersection.
 * Alternatively, if it is not on a set bearing, we find the nearest point on the parent line to the child point and shift the child point to that nearest point.
 * In both cases we also update the connecting line so that it connects to the new shifted point.
 *
 * @param childPoint the start/end {@link Point} of the child geodesic line.
 * @param nearestCoordinate the nearest {@link Point} on the parent line.
 * @param childId the id of the child geodesic line.
 * @param idToLineWrapper a map of all children {@link LineWithNavigationTypeAndId} to their id.
 * @param parentGeodesicLine the parent geodesic {@link Polyline}
 * @param nodeType for debugging so we know if the point passed was for the start of the end of the line.
 * @returns the {@link Point} that the child point was shifted to, which will either be the intersection point between the
 * parent line and the extended connecting line that was on bearing, or the nearest point on the parent line if the connecting
 * line was not on bearing.
 */
export function shiftNodeAndUpdateConnectedLine(
  childPoint: Point,
  nearestCoordinate: Point,
  childId: number,
  idToLineWrapper: Map<number, LineWithNavigationTypeAndId>,
  parentGeodesicLine: Polyline,
  nodeType: 'start' | 'end',
): Point {
  const linesWithNavigationTypeAndId = Array.from(idToLineWrapper.values());
  const lineOnBearing = findLoxodromeThatConnectsToPointOnSetBearing(childPoint, linesWithNavigationTypeAndId);

  if (lineOnBearing !== undefined) {
    logger.info(`Line  ${childId} connected to id: ${lineOnBearing.id} ${nodeType} node is on set bearing `);
    const newPoint = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
      childPoint,
      lineOnBearing.setBearing,
      parentGeodesicLine,
      ONE_ARC_SECOND,
    );

    if (newPoint === undefined) {
      throw new Error(`No intersection point for line ${lineOnBearing.id} on set bearing was found.`);
    }

    // Update the node on the connecting line and add it to the processed lines
    const index = getIndexOfPointOnLine(childPoint, lineOnBearing.line);
    lineOnBearing.line.setPoint(0, index, newPoint);
    const bearingLineEntry = idToLineWrapper.get(lineOnBearing.id);
    if (bearingLineEntry === undefined) {
      throw new Error(`Line with id ${lineOnBearing.id} not found in idToLineWrapper`);
    }
    bearingLineEntry.line = lineOnBearing.line;
    return newPoint;
  }

  const lineConnectingToPoint = findLineConnectingToPointNotOnBearing(childPoint, childId, linesWithNavigationTypeAndId);
  if (lineConnectingToPoint === undefined) {
    throw new Error(`No line connecting to ${nodeType} node with id ${childId} was found.`);
  }

  logger.debug(`Line ${childId} connected to id: ${lineConnectingToPoint.id} ${nodeType} node is NOT on set bearing`);

  const index = getIndexOfPointOnLine(childPoint, lineConnectingToPoint.line);
  lineConnectingToPoint.line.setPoint(0, index, nearestCoordinate);
  const connectingLineEntry = idToLineWrapper.get(lineConnectingToPoint.id);
  if (connectingLineEntry === undefined) {
    throw new Error(`Line with id ${lineConnectingToPoint.id} not found in idToLineWrapper`);
  }
  connectingLineEntry.line = lineConnectingToPoint.line;
  return nearestCoordinate;
}

/**
 * This takes a point and extends the point by {@link pointExtension} in both directions along the given {@link SetBearing}
 * to try and find an intersection with the {@link parent} {@link Polyline}.
 *
 * @param childPoint the point we want to extend to try and find an intersection.
 * @param bearing the direction we want to extend the point in.
 * @param parent the line that we want to intersect.
 * @param pointExtension the amount we want to extend the {@link childPoint} by in each direction,
 * uses the same unit as the spatial reference of the parent line.
 * @returns the {@link Point} of intersection if there is an intersection, or undefined if there is no intersection.
 */
export function findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
  childPoint: Point,
  bearing: SetBearing,
  parent: Polyline,
  pointExtension: number,
): Point | undefined {
  if (typeof childPoint.longitude !== 'number' || typeof childPoint.latitude !== 'number') {
    return undefined;
  }

  let twoSecondLine: Polyline;
  switch (bearing) {
    case SetBearing.LATITUDE:
      twoSecondLine = pointToNorthSouthLine(childPoint.longitude, childPoint.latitude, parent.spatialReference, pointExtension);

      break;
    case SetBearing.LONGITUDE:
      twoSecondLine = pointToEastWestLine(childPoint.longitude, childPoint.latitude, parent.spatialReference, pointExtension);
      break;
  }

  if (!intersectsOperator.execute(parent, twoSecondLine)) {
    return undefined;
  }

  // Using normal .execute doesn't work unless there is an overlap between the two lines to create a new polyline
  // if there is just an intersection the result should be Point/Multipoint which is a lower dimension, so the .execute just
  // returns null.
  // see https://developers.arcgis.com/javascript/latest/api-reference/esri-geometry-geometryEngine.html#intersectLinesToPoints
  const intersectionResult = intersectionOperator.executeMany([parent], twoSecondLine);

  // return the first point of intersection
  return (intersectionResult[0] as Multipoint).getPoint(0) as Point;
}

/**
 * This finds the line that connects to the {@link point} which is not on a set bearing.
 * @param point the start/end {@link Point} of a line we are looking for a connection for.
 * @param targetLineId the id of the line we are looking for the connection for (so we can exclude it from the search)
 * @param lines a list of {@link LineWithNavigationTypeAndId} which are possible lines that connect to our {@link point}
 * @returns the {@link LineWithNavigationTypeAndId} which connects to the point, or undefined if no such line exists.
 */
export function findLineConnectingToPointNotOnBearing(
  point: Point,
  targetLineId: number,
  lines: LineWithNavigationTypeAndId[],
): LineWithNavigationTypeAndId | undefined {
  return lines.find((lineWrapper: LineWithNavigationTypeAndId) => {
    if (lineWrapper.id === targetLineId) {
      return false;
    }
    const line = lineWrapper.line;
    const startPoint = line.getPoint(0, 0);
    const endPoint = line.getPoint(0, line.paths[0].length - 1);

    if (point.equals(startPoint)) {
      return true;
    } else return point.equals(endPoint);
  });
}
